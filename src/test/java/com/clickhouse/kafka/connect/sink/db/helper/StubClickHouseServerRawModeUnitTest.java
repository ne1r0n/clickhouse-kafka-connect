package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Docker-free premise pins of the raw-socket mode of {@link StubClickHouseServer}. The later insert pins
 * rely on behaviours the HTTP mode cannot produce, so each is shown here against a plain socket client.
 * First, a peer that reads only the request head blocks a large body write.
 */
@Timeout(60)
class StubClickHouseServerRawModeUnitTest {

    private static final String LOOPBACK = "127.0.0.1";
    private static final String CRLF = "\r\n";
    private static final int MEBIBYTE = 1024 * 1024;
    private static final int STALL_BODY_CHUNKS = 64;
    private static final Duration STALL_OBSERVATION = Duration.ofSeconds(2);
    private static final Duration RELEASE_BOUND = Duration.ofSeconds(5);
    private static final Duration AWAIT_BOUND = Duration.ofSeconds(10);
    private static final Duration POLL = Duration.ofMillis(10);
    private static final String POST_LINE = "POST /?query=INSERT%20INTO%20t&max_block_size=1 HTTP/1.1" + CRLF;
    private static final String CHUNKED_POST_HEAD =
            POST_LINE + "Host: 127.0.0.1" + CRLF + "Transfer-Encoding: chunked" + CRLF + CRLF;
    private static final String CHUNKED_TERMINATOR = "0" + CRLF + CRLF;
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
}
