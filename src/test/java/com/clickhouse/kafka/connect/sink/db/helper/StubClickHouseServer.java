package com.clickhouse.kafka.connect.sink.db.helper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-JVM HTTP stub for the client pins: records every request and answers each with one scripted
 * response. It speaks just enough of ClickHouse's HTTP interface to drive the real client over a real
 * socket, so no Docker is needed. It can also hold every request until it closes, or answer OK after a
 * delay, on a daemon pool so every request is recorded while one is held.
 *
 * <p>A second, raw-socket mode (the factory {@link #notReadingBody()}) covers what the HTTP server cannot:
 * it reads the whole request body before a responder runs, so it can never stall the write side of a client.
 * The raw mode listens with a small receive buffer so a peer that stops reading blocks a writer after few
 * bytes on every OS, and shares {@link #port()}, {@link #requests()} and {@link #close()} with the HTTP mode.
 */
public final class StubClickHouseServer implements AutoCloseable {

    public record Request(String method, String rawUri, Map<String, String> headers, byte[] body) {}

    @FunctionalInterface
    public interface Responder {
        void respond(HttpExchange exchange) throws IOException;
    }

    private static final String EXCEPTION_CODE_HEADER = "X-ClickHouse-Exception-Code";
    private static final String SUMMARY_HEADER = "X-ClickHouse-Summary";
    private static final String SUMMARY_ONE_ROW = "{\"written_rows\":\"1\",\"written_bytes\":\"3\"}";
    private static final String LOOPBACK = "127.0.0.1";
    private static final Duration STALL_CAP = Duration.ofSeconds(60);
    private static final String HANDLER_THREAD_NAME = "stub-clickhouse-handler";
    private static final String RAW_THREAD_NAME = "stub-clickhouse-raw";
    private static final int RAW_RECEIVE_BUFFER_BYTES = 4096;
    private static final int RAW_ACCEPT_BACKLOG = 50;
    private static final int RAW_HEAD_LIMIT_BYTES = 65536;
    private static final String CRLF = "\r\n";
    private static final String HEAD_END = "\r\n\r\n";

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private final CountDownLatch release;
    private final ExecutorService handlers;
    private final AtomicBoolean closed = new AtomicBoolean();

    private final ServerSocket rawServer;
    private final RawHandler rawHandler;
    private final List<Socket> rawSockets = new CopyOnWriteArrayList<>();
    private final List<Thread> rawThreads = new CopyOnWriteArrayList<>();
    private final AtomicLong bodyBytesRead = new AtomicLong();

    private record Head(String method, String rawUri, Map<String, String> headers) {}

    @FunctionalInterface
    private interface RawHandler {
        void handle(StubClickHouseServer stub, Socket socket) throws IOException, InterruptedException;
    }

    public StubClickHouseServer(Responder responder) throws IOException {
        this(responder, new CountDownLatch(1), null);
    }

    private StubClickHouseServer(Responder responder, CountDownLatch release, ExecutorService handlers)
            throws IOException {
        this.release = release;
        this.handlers = handlers;
        this.rawServer = null;
        this.rawHandler = null;
        server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0);
        server.createContext("/", exchange -> {
            Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, values.get(0)));
            requests.add(new Request(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().toString(),
                    headers,
                    exchange.getRequestBody().readAllBytes()));
            responder.respond(exchange);
        });
        if (handlers != null) {
            server.setExecutor(handlers);
        }
        server.start();
    }

    public static StubClickHouseServer stalling() throws IOException {
        CountDownLatch release = new CountDownLatch(1);
        return new StubClickHouseServer(
                exchange -> {
                    awaitRelease(release, STALL_CAP);
                    exchange.close();
                },
                release,
                handlerPool());
    }

    public static StubClickHouseServer okAfter(Duration delay) throws IOException {
        CountDownLatch release = new CountDownLatch(1);
        Responder answer = ok();
        return new StubClickHouseServer(
                exchange -> {
                    awaitRelease(release, delay);
                    answer.respond(exchange);
                },
                release,
                handlerPool());
    }

    private StubClickHouseServer(RawHandler rawHandler, String threadName) throws IOException {
        this.release = new CountDownLatch(1);
        this.handlers = null;
        this.server = null;
        this.rawHandler = rawHandler;
        rawServer = new ServerSocket();
        // The receive buffer is set before bind so accepted sockets inherit it and loopback buffering stays small.
        rawServer.setReceiveBufferSize(RAW_RECEIVE_BUFFER_BYTES);
        rawServer.bind(new InetSocketAddress(LOOPBACK, 0), RAW_ACCEPT_BACKLOG);
        startRawThread(threadName, this::acceptLoop);
    }

    /** Reads the request head only, records the request with an empty body, then reads nothing until close. */
    public static StubClickHouseServer notReadingBody() throws IOException {
        return new StubClickHouseServer(
                (stub, socket) -> {
                    stub.readAndRecordHead(socket);
                    awaitRelease(stub.release, STALL_CAP);
                },
                RAW_THREAD_NAME);
    }

    /** Total request-body bytes the raw mode has read; chunk framing is not counted. */
    public long bodyBytesRead() {
        return bodyBytesRead.get();
    }

    public int port() {
        return server != null ? server.getAddress().getPort() : rawServer.getLocalPort();
    }

    public List<Request> requests() {
        return requests;
    }

    public static Responder status(int status, String body) {
        return exchange -> send(exchange, status, body);
    }

    public static Responder serverException(int code, String body) {
        return exchange -> {
            exchange.getResponseHeaders().add(EXCEPTION_CODE_HEADER, String.valueOf(code));
            send(exchange, 500, body);
        };
    }

    public static Responder ok() {
        return exchange -> {
            exchange.getResponseHeaders().add(SUMMARY_HEADER, SUMMARY_ONE_ROW);
            send(exchange, 200, "");
        };
    }

    private static ExecutorService handlerPool() {
        return Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task, HANDLER_THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
    }

    private static void awaitRelease(CountDownLatch release, Duration wait) {
        try {
            release.await(wait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        // A held request is released before the stop: a handler waiting on the latch would otherwise keep
        // its exchange open, and on the server's own dispatcher thread would keep stop(0) from returning.
        release.countDown();
        if (server != null) {
            server.stop(0);
        }
        if (handlers != null) {
            handlers.shutdownNow();
        }
        if (rawServer != null) {
            closeRaw();
        }
    }

    // Closing every accepted socket with a reset fails a client blocked in a body write at once, and
    // interrupting the raw threads ends the ones that are sleeping between reads or progress lines.
    private void closeRaw() {
        closeQuietly(rawServer);
        for (Socket socket : rawSockets) {
            try {
                socket.setSoLinger(true, 0);
            } catch (IOException e) {
                // closed already
            }
            closeQuietly(socket);
        }
        for (Thread thread : rawThreads) {
            thread.interrupt();
        }
    }

    private static void closeQuietly(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            // best effort on shutdown
        }
    }

    @FunctionalInterface
    private interface RawTask {
        void run();
    }

    private Thread startRawThread(String name, RawTask task) {
        Thread thread = new Thread(task::run, name);
        thread.setDaemon(true);
        rawThreads.add(thread);
        thread.start();
        return thread;
    }

    private void acceptLoop() {
        while (!closed.get()) {
            Socket socket;
            try {
                socket = rawServer.accept();
            } catch (IOException e) {
                return;
            }
            rawSockets.add(socket);
            startRawThread(RAW_THREAD_NAME, () -> serveRaw(socket));
        }
    }

    private void serveRaw(Socket socket) {
        try {
            rawHandler.handle(this, socket);
        } catch (IOException e) {
            // the client or close() ended the connection
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (closed.get()) {
                closeQuietly(socket);
            }
        }
    }

    private Head readAndRecordHead(Socket socket) throws IOException {
        Head head = readHead(socket);
        requests.add(toRequest(head, new byte[0]));
        return head;
    }

    private Request toRequest(Head head, byte[] body) {
        return new Request(head.method(), head.rawUri(), head.headers(), body);
    }

    // Byte by byte, so no body byte is consumed beyond the blank line that ends the head.
    private Head readHead(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        int matched = 0;
        while (matched < HEAD_END.length()) {
            int next = in.read();
            if (next < 0) {
                throw new EOFException("connection closed inside the request head");
            }
            raw.write(next);
            if (raw.size() > RAW_HEAD_LIMIT_BYTES) {
                throw new IOException("request head longer than " + RAW_HEAD_LIMIT_BYTES + " bytes");
            }
            matched = next == HEAD_END.charAt(matched) ? matched + 1 : (next == HEAD_END.charAt(0) ? 1 : 0);
        }
        String[] lines = raw.toString(StandardCharsets.ISO_8859_1).split(CRLF);
        String[] requestLine = lines[0].split(" ");
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0) {
                headers.put(lines[i].substring(0, colon).trim(), lines[i].substring(colon + 1).trim());
            }
        }
        return new Head(requestLine[0], requestLine.length > 1 ? requestLine[1] : "", headers);
    }
}
