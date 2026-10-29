package com.clickhouse.kafka.connect.sink.db.helper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-JVM HTTP stub for the client pins: records every request and answers each with one scripted
 * response. It speaks just enough of ClickHouse's HTTP interface to drive the real client over a real
 * socket, so no Docker is needed.
 */
final class StubClickHouseServer implements AutoCloseable {

    record Request(String method, String rawUri, Map<String, String> headers, byte[] body) {}

    @FunctionalInterface
    interface Responder {
        void respond(HttpExchange exchange) throws IOException;
    }

    private static final String EXCEPTION_CODE_HEADER = "X-ClickHouse-Exception-Code";
    private static final String SUMMARY_HEADER = "X-ClickHouse-Summary";
    private static final String SUMMARY_ONE_ROW = "{\"written_rows\":\"1\",\"written_bytes\":\"3\"}";
    private static final String LOOPBACK = "127.0.0.1";

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    StubClickHouseServer(Responder responder) throws IOException {
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
        server.start();
    }

    int port() {
        return server.getAddress().getPort();
    }

    List<Request> requests() {
        return requests;
    }

    static Responder status(int status, String body) {
        return exchange -> send(exchange, status, body);
    }

    static Responder serverException(int code, String body) {
        return exchange -> {
            exchange.getResponseHeaders().add(EXCEPTION_CODE_HEADER, String.valueOf(code));
            send(exchange, 500, body);
        };
    }

    static Responder ok() {
        return exchange -> {
            exchange.getResponseHeaders().add(SUMMARY_HEADER, SUMMARY_ONE_ROW);
            send(exchange, 200, "");
        };
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
        server.stop(0);
    }
}
