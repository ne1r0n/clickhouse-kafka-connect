package com.clickhouse.kafka.connect.sink.db.helper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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

/**
 * In-JVM HTTP stub for the client pins: records every request and answers each with one scripted
 * response. It speaks just enough of ClickHouse's HTTP interface to drive the real client over a real
 * socket, so no Docker is needed. It can also hold every request until it closes, or answer OK after a
 * delay, on a daemon pool so every request is recorded while one is held.
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

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private final CountDownLatch release;
    private final ExecutorService handlers;
    private final AtomicBoolean closed = new AtomicBoolean();

    public StubClickHouseServer(Responder responder) throws IOException {
        this(responder, new CountDownLatch(1), null);
    }

    private StubClickHouseServer(Responder responder, CountDownLatch release, ExecutorService handlers)
            throws IOException {
        this.release = release;
        this.handlers = handlers;
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

    public int port() {
        return server.getAddress().getPort();
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
        server.stop(0);
        if (handlers != null) {
            handlers.shutdownNow();
        }
    }
}
