package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Docker-free premise pins of the raw-socket mode of {@link StubClickHouseServer}. The later insert pins
 * rely on four behaviours the HTTP mode cannot produce, so each is shown here against a plain socket
 * client: a peer that reads only the request head blocks a large body write, a peer that reads the body
 * slowly keeps reading without answering, a peer streams progress headers over time, and a peer streams
 * them while it is still reading the body.
 */
@Timeout(60)
class StubClickHouseServerRawModeUnitTest {

    private static final String LOOPBACK = "127.0.0.1";
    private static final String CRLF = "\r\n";
    private static final int MEBIBYTE = 1024 * 1024;
    private static final int STALL_BODY_CHUNKS = 64;
    private static final int SLOW_BODY_CHUNKS = 8;
    private static final long SLOW_BODY_BYTES = (long) SLOW_BODY_CHUNKS * MEBIBYTE;
    private static final int SLOW_BYTES_PER_TICK = 65536;
    private static final Duration SLOW_TICK = Duration.ofMillis(20);
    private static final Duration PROGRESS_INTERVAL = Duration.ofMillis(100);
    private static final Duration PROGRESS_TOTAL = Duration.ofSeconds(1);
    private static final Duration STALL_OBSERVATION = Duration.ofSeconds(2);
    private static final Duration RELEASE_BOUND = Duration.ofSeconds(5);
    private static final Duration AWAIT_BOUND = Duration.ofSeconds(10);
    private static final Duration POLL = Duration.ofMillis(10);
    private static final Duration FIRST_SAMPLE = Duration.ofMillis(500);
    private static final Duration SECOND_SAMPLE_GAP = Duration.ofSeconds(1);
    private static final int NO_RESPONSE_PROBE_MS = 200;
    private static final long MIN_PROGRESS_SPREAD_MS = 700;
    private static final int MIN_PROGRESS_LINES = 8;
    private static final int MIN_PROGRESS_LINES_WHILE_READING = 3;
    private static final String OK_STATUS_LINE = "HTTP/1.1 200 OK";
    private static final String PROGRESS_PREFIX = "X-ClickHouse-Progress:";
    private static final String SUMMARY_PREFIX = "X-ClickHouse-Summary:";
    private static final String POST_LINE = "POST /?query=INSERT%20INTO%20t&max_block_size=1 HTTP/1.1" + CRLF;
    private static final String CHUNKED_POST_HEAD =
            POST_LINE + "Host: 127.0.0.1" + CRLF + "Transfer-Encoding: chunked" + CRLF + CRLF;
    private static final String CHUNKED_TERMINATOR = "0" + CRLF + CRLF;
    private static final byte[] SMALL_BODY = "id,name\n1,a\n".getBytes(StandardCharsets.US_ASCII);
    private static final String CUSTOM_HEADER_SENT = "X-Custom-Header: Custom Value" + CRLF;
    private static final String CUSTOM_HEADER_LOOKUP = "x-custom-header";
    private static final String CUSTOM_HEADER_VALUE = "Custom Value";

    @Test
    void notReadingBodyBlocksA64MebibyteBodyWrite() throws Exception {
        BodyWriter writer;
        try (StubClickHouseServer stub = StubClickHouseServer.notReadingBody();
                Socket client = new Socket(LOOPBACK, stub.port())) {
            writer = BodyWriter.start(client, STALL_BODY_CHUNKS, MEBIBYTE);
            awaitRequests(stub, 1);
            Thread.sleep(STALL_OBSERVATION.toMillis());

            assertFalse(writer.finished.get(), "the body write must still be blocked");
            assertEquals(1, writer.done.getCount(), "the writer thread must still be running");
            assertEquals(1, stub.requests().size());
            assertEquals(0, stub.bodyBytesRead());

            stub.close();
            assertTrue(
                    writer.done.await(RELEASE_BOUND.toMillis(), TimeUnit.MILLISECONDS),
                    "close() must release the blocked writer");
        }
        assertFalse(writer.finished.get());
        assertNotNull(writer.failure.get(), "the blocked write must end with an IOException");
    }

    @Test
    void rawModeRecordsTheRequestLineAndHeaders() throws Exception {
        byte[] body = "abc".getBytes(StandardCharsets.US_ASCII);
        try (StubClickHouseServer stub = StubClickHouseServer.notReadingBody();
                Socket client = new Socket(LOOPBACK, stub.port())) {
            sendContentLengthPost(client, body);
            awaitRequests(stub, 1);

            StubClickHouseServer.Request request = stub.requests().get(0);
            assertEquals("POST", request.method());
            assertEquals("/?query=INSERT%20INTO%20t&max_block_size=1", request.rawUri());
            assertEquals(CUSTOM_HEADER_VALUE, request.headers().get(CUSTOM_HEADER_LOOKUP));
            assertEquals(String.valueOf(body.length), request.headers().get("CONTENT-LENGTH"));
            assertEquals(0, request.body().length);
        }
    }

    @Test
    void readingBodySlowlyKeepsReadingWithoutAnswering() throws Exception {
        try (StubClickHouseServer stub = StubClickHouseServer.readingBodySlowly(SLOW_BYTES_PER_TICK, SLOW_TICK);
                Socket client = new Socket(LOOPBACK, stub.port())) {
            BodyWriter writer = BodyWriter.start(client, SLOW_BODY_CHUNKS, MEBIBYTE);
            awaitRequests(stub, 1);
            Thread.sleep(FIRST_SAMPLE.toMillis());
            long first = stub.bodyBytesRead();
            Thread.sleep(SECOND_SAMPLE_GAP.toMillis());
            long second = stub.bodyBytesRead();

            assertTrue(first > 0, "the stub must be reading: " + first);
            assertTrue(second > first, "the stub must keep reading: " + first + " then " + second);
            assertTrue(second < SLOW_BODY_BYTES, "the body must not be read through yet: " + second);
            assertFalse(writer.finished.get());
            client.setSoTimeout(NO_RESPONSE_PROBE_MS);
            assertThrows(SocketTimeoutException.class, () -> client.getInputStream().read());
        }
    }

    @Test
    void progressHeadersStreamsHeadersOverTimeThenOk() throws Exception {
        try (StubClickHouseServer stub = StubClickHouseServer.progressHeaders(PROGRESS_INTERVAL, PROGRESS_TOTAL);
                Socket client = new Socket(LOOPBACK, stub.port())) {
            ResponseReader response = ResponseReader.start(client);
            sendChunkedPost(client, SMALL_BODY);

            assertProgressResponse(stub, response, SMALL_BODY);
        }
    }

    @Test
    void progressHeadersReadsAContentLengthBody() throws Exception {
        try (StubClickHouseServer stub = StubClickHouseServer.progressHeaders(PROGRESS_INTERVAL, PROGRESS_TOTAL);
                Socket client = new Socket(LOOPBACK, stub.port())) {
            ResponseReader response = ResponseReader.start(client);
            sendContentLengthPost(client, SMALL_BODY);

            assertProgressResponse(stub, response, SMALL_BODY);
        }
    }

    @Test
    void progressWhileReadingBodySlowlyAnswersBeforeTheBodyEnds() throws Exception {
        try (StubClickHouseServer stub = StubClickHouseServer.progressWhileReadingBodySlowly(
                        SLOW_BYTES_PER_TICK, SLOW_TICK, PROGRESS_INTERVAL);
                Socket client = new Socket(LOOPBACK, stub.port())) {
            ResponseReader response = ResponseReader.start(client);
            BodyWriter writer = BodyWriter.start(client, SLOW_BODY_CHUNKS, MEBIBYTE);

            await(
                    () -> response.progressLines() >= MIN_PROGRESS_LINES_WHILE_READING,
                    "the progress lines must arrive while the body is still being sent");

            assertEquals(OK_STATUS_LINE, response.lines.get(0));
            assertTrue(stub.bodyBytesRead() < SLOW_BODY_BYTES, "the body must still be being read");
            assertFalse(writer.finished.get(), "the client must still be sending the body");
        }
    }

    private static void assertProgressResponse(
            StubClickHouseServer stub, ResponseReader response, byte[] sentBody) throws InterruptedException {
        assertTrue(
                response.ended.await(AWAIT_BOUND.toMillis(), TimeUnit.MILLISECONDS),
                "the response must end after the progress lines");
        assertEquals(OK_STATUS_LINE, response.lines.get(0));
        long progress = response.progressLines();
        assertTrue(progress >= MIN_PROGRESS_LINES, "progress lines: " + progress);
        long spreadMs = TimeUnit.NANOSECONDS.toMillis(response.lastProgressNanos() - response.firstProgressNanos());
        assertTrue(spreadMs >= MIN_PROGRESS_SPREAD_MS, "progress lines spread over " + spreadMs + " ms");
        assertTrue(response.lines.stream().anyMatch(line -> line.startsWith(SUMMARY_PREFIX)), "summary line");
        assertTrue(response.lines.contains(""), "the end of the headers");
        assertEquals(1, stub.requests().size());
        assertArrayEquals(sentBody, stub.requests().get(0).body());
    }

    private static void sendChunkedPost(Socket client, byte[] body) throws IOException {
        OutputStream out = client.getOutputStream();
        out.write(CHUNKED_POST_HEAD.getBytes(StandardCharsets.US_ASCII));
        writeChunk(out, body, body.length);
        out.write(CHUNKED_TERMINATOR.getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private static void sendContentLengthPost(Socket client, byte[] body) throws IOException {
        OutputStream out = client.getOutputStream();
        String head = POST_LINE + "Host: 127.0.0.1" + CRLF + CUSTOM_HEADER_SENT + "Content-Length: " + body.length + CRLF
                + CRLF;
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    private static void writeChunk(OutputStream out, byte[] data, int length) throws IOException {
        out.write((Integer.toHexString(length) + CRLF).getBytes(StandardCharsets.US_ASCII));
        out.write(data, 0, length);
        out.write(CRLF.getBytes(StandardCharsets.US_ASCII));
    }

    private static void awaitRequests(StubClickHouseServer stub, int count) throws InterruptedException {
        await(() -> stub.requests().size() >= count, "the stub must record " + count + " request(s)");
    }

    private static void await(BooleanSupplier condition, String message) throws InterruptedException {
        long deadline = System.nanoTime() + AWAIT_BOUND.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail(message);
            }
            Thread.sleep(POLL.toMillis());
        }
    }

    /** Writes a chunked POST of {@code chunks} chunks of one reused buffer on a daemon thread. */
    private static final class BodyWriter {
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicBoolean finished = new AtomicBoolean();
        final AtomicReference<IOException> failure = new AtomicReference<>();

        static BodyWriter start(Socket client, int chunks, int chunkBytes) {
            BodyWriter writer = new BodyWriter();
            Thread thread = new Thread(() -> writer.run(client, chunks, chunkBytes), "raw-mode-test-writer");
            thread.setDaemon(true);
            thread.start();
            return writer;
        }

        private void run(Socket client, int chunks, int chunkBytes) {
            try {
                OutputStream out = client.getOutputStream();
                out.write(CHUNKED_POST_HEAD.getBytes(StandardCharsets.US_ASCII));
                byte[] chunk = new byte[chunkBytes];
                Arrays.fill(chunk, (byte) 'x');
                for (int i = 0; i < chunks; i++) {
                    writeChunk(out, chunk, chunkBytes);
                }
                out.write(CHUNKED_TERMINATOR.getBytes(StandardCharsets.US_ASCII));
                out.flush();
                finished.set(true);
            } catch (IOException e) {
                failure.set(e);
            } finally {
                done.countDown();
            }
        }
    }

    /** Reads the response line by line on a daemon thread and stamps each line's arrival. */
    private static final class ResponseReader {
        final List<String> lines = new CopyOnWriteArrayList<>();
        final List<Long> arrivals = new CopyOnWriteArrayList<>();
        final CountDownLatch ended = new CountDownLatch(1);

        static ResponseReader start(Socket client) {
            ResponseReader reader = new ResponseReader();
            Thread thread = new Thread(() -> reader.run(client), "raw-mode-test-reader");
            thread.setDaemon(true);
            thread.start();
            return reader;
        }

        private void run(Socket client) {
            try (BufferedReader in =
                    new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1))) {
                String line;
                while ((line = in.readLine()) != null) {
                    arrivals.add(System.nanoTime());
                    lines.add(line);
                }
            } catch (IOException e) {
                // the stub closed the connection
            } finally {
                ended.countDown();
            }
        }

        long progressLines() {
            return lines.stream().filter(line -> line.startsWith(PROGRESS_PREFIX)).count();
        }

        long firstProgressNanos() {
            return arrivals.get(indexOfProgress(false));
        }

        long lastProgressNanos() {
            return arrivals.get(indexOfProgress(true));
        }

        private int indexOfProgress(boolean last) {
            int found = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith(PROGRESS_PREFIX)) {
                    found = i;
                    if (!last) {
                        break;
                    }
                }
            }
            return found;
        }
    }
}
