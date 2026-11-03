package com.clickhouse.kafka.connect.sink.util;

import com.clickhouse.client.api.ServerException;

/**
 * Builds a {@link ServerException} on every client line the connector can ship. Client 0.9.6 has
 * only the (code, message, transport status) constructor and client 0.11.0 only the four-argument
 * one with a query id, so one reflective lookup keeps the call sites compiling on both and keeps the
 * catalog bump a one-commit revert.
 */
public final class ServerExceptions {

    private static final String NO_QUERY_ID = "";

    private ServerExceptions() {}

    public static ServerException create(int code, String message, int transportStatus) {
        try {
            try {
                return ServerException.class
                        .getConstructor(int.class, String.class, int.class, String.class)
                        .newInstance(code, message, transportStatus, NO_QUERY_ID);
            } catch (NoSuchMethodException withoutQueryId) {
                return ServerException.class
                        .getConstructor(int.class, String.class, int.class)
                        .newInstance(code, message, transportStatus);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("ServerException has neither known constructor", e);
        }
    }
}
