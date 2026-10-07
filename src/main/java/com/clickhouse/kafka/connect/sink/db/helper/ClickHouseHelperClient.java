package com.clickhouse.kafka.connect.sink.db.helper;

import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseNodeSelector;
import com.clickhouse.client.ClickHouseProtocol;
import com.clickhouse.client.ClickHouseRequest;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.enums.ProxyType;
import com.clickhouse.client.api.insert.InsertSettings;
import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.QueryResponse;
import com.clickhouse.client.api.query.QuerySettings;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.client.config.ClickHouseClientOption;
import com.clickhouse.client.config.ClickHouseProxyType;
import com.clickhouse.client.config.ClickHouseSslMode;
import com.clickhouse.client.http.config.ClickHouseHttpOption;
import com.clickhouse.config.ClickHouseOption;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.data.ClickHouseRecord;
import com.clickhouse.data.ClickHouseValue;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.Version;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClickHouseHelperClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseHelperClient.class);

    private final String hostname;
    private final int port;
    @Getter
    private final String username;
    @Getter
    private final String database;
    @Getter
    private final String password;
    private final boolean sslEnabled;
    private final String jdbcConnectionProperties;
    private final int timeout;
    @Getter
    private ClickHouseNode server = null;
    @Getter
    private Client client = null;
    private final int retry;
    private ClickHouseProxyType proxyType = null;
    private String proxyHost = null;
    private int proxyPort = -1;
    @Getter
    private boolean useClientV2 = false;
    private final String sslSocketSni;
    private final String clusterClause;
    @Getter
    private final boolean enableReplicaPinning;
    private final String targetTableFilter;
    @Getter
    private final boolean setCompressionMethod;

    // Part of HTTP protocol header
    public static final String REPLICA_TAG_HEADER = "X-ClickHouse-Replica-Tag";
    public static final String NETWORK_COMPRESSION_METHOD = "network_compression_method";
    public static final String LZ4 = "lz4";

    public ClickHouseHelperClient(ClickHouseClientBuilder builder) {
        this.hostname = builder.hostname;
        this.port = builder.port;
        this.username = builder.username;
        this.password = builder.password;
        this.database = builder.database;
        this.sslEnabled = builder.sslEnabled;
        this.jdbcConnectionProperties = builder.jdbcConnectionProperties;
        this.timeout = builder.timeout;
        this.retry = builder.retry;
        this.proxyType = builder.proxyType;
        this.proxyHost = builder.proxyHost;
        this.proxyPort = builder.proxyPort;
        this.useClientV2 = builder.useClientV2;
        this.sslSocketSni = builder.sslSocketSni;
        this.clusterClause = builder.clusterClause;
        this.enableReplicaPinning = builder.enableReplicaPinning;
        this.targetTableFilter = builder.targetTableFilter;
        this.setCompressionMethod = builder.setCompressionMethod;
        // We are creating two clients, one for V1 and one for V2
        this.client = createClientV2();
        this.server = createClientV1();
    }

    public static final String CONNECT_CLIENT_NAME = "clickhouse-kafka-connect/" + Version.ARTIFACT_VERSION;

    // ClickHouse 26.9 defaults network_compression_method to ZSTD, which client 0.9.6 cannot decode.
    // Remove once the pinned client decodes ZSTD responses.
    static final String NETWORK_COMPRESSION_METHOD_SETTING = "network_compression_method";
    static final String NETWORK_COMPRESSION_METHOD_LZ4 = "lz4";

    public Map<ClickHouseOption, Serializable> getDefaultClientOptions() {
        Map<ClickHouseOption, Serializable> options = new HashMap<>();
        options.put(ClickHouseClientOption.CLIENT_NAME, CONNECT_CLIENT_NAME);
        if (setCompressionMethod) {
            options.put(ClickHouseClientOption.CUSTOM_SETTINGS, NETWORK_COMPRESSION_METHOD + "=" + LZ4);
        }
        options.put(ClickHouseClientOption.COMPRESS, false);
        if (proxyType != null && !proxyType.equals(ClickHouseProxyType.IGNORE)) {
            options.put(ClickHouseClientOption.PROXY_TYPE, proxyType);
            options.put(ClickHouseClientOption.PROXY_HOST, proxyHost);
            options.put(ClickHouseClientOption.PROXY_PORT, proxyPort);
        }
        return options;
    }

    private ClickHouseNode createClientV1() {
        String protocol = "http";
        if (this.sslEnabled)
            protocol += "s";

        String tmpJdbcConnectionProperties = jdbcConnectionProperties;
        if (tmpJdbcConnectionProperties != null && !tmpJdbcConnectionProperties.startsWith("?")) {
            tmpJdbcConnectionProperties = "?" + tmpJdbcConnectionProperties;
        }

        String url = String.format("%s://%s:%d/%s%s",
                protocol,
                hostname,
                port,
                database,
                tmpJdbcConnectionProperties
        );

        LOGGER.info("ClickHouse URL: {}", url);

        final Map<String, String> options = new HashMap<>();
        if (setCompressionMethod) {
            options.put(NETWORK_COMPRESSION_METHOD, LZ4);
        }
        options.put(ClickHouseClientOption.COMPRESS.getKey(), "false");
        if (username != null && password != null) {
            LOGGER.debug(String.format("Adding username [%s]", username));
            options.put("user", username);
            options.put("password", password);
        }
        if (sslSocketSni != null && !sslSocketSni.isEmpty()) {
            options.put(ClickHouseClientOption.SSL_SOCKET_SNI.getKey(), sslSocketSni);
            options.put(ClickHouseClientOption.SSL_MODE.getKey(), ClickHouseSslMode.NONE.name().toLowerCase()); // disable hostname/cert validation (matches client v2 behavior - see HttpAPIClientHelper.createHttpClient)
        }
        server = ClickHouseNode.of(url, options);
        return server;
    }

    private Client createClientV2() {
        String protocol = "http";
        if (this.sslEnabled)
            protocol += "s";

        String tmpJdbcConnectionProperties = jdbcConnectionProperties;
        if (tmpJdbcConnectionProperties != null && !tmpJdbcConnectionProperties.startsWith("?")) {
            tmpJdbcConnectionProperties = "?" + tmpJdbcConnectionProperties;
        }

        String url = String.format("%s://%s:%d/%s%s",
                protocol,
                hostname,
                port,
                database,
                tmpJdbcConnectionProperties
        );

        LOGGER.info("ClickHouse URL: {}", url);


        Client.Builder clientBuilder = new Client.Builder()
                .addEndpoint(url)
                .setUsername(this.username)
                .setPassword(this.password)
                .setClientName(CONNECT_CLIENT_NAME)
                .compressServerResponse(false)
                .setDefaultDatabase(this.database);

        if (setCompressionMethod) {
            clientBuilder.serverSetting(NETWORK_COMPRESSION_METHOD, LZ4);
        }

        if (jdbcConnectionProperties != null && !jdbcConnectionProperties.isEmpty()) {
            String props = jdbcConnectionProperties.startsWith("?") ? jdbcConnectionProperties.substring(1) : jdbcConnectionProperties;
            for (String pair : props.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && !kv[0].isEmpty()) {
                    clientBuilder.setOption(kv[0], kv[1]);
                }
            }
        }

        if (proxyType != null && !proxyType.equals(ClickHouseProxyType.IGNORE)) {
            clientBuilder.addProxy(ProxyType.HTTP, proxyHost, proxyPort);
        }
        if (sslSocketSni != null && !sslSocketSni.isEmpty()) {
            clientBuilder.sslSocketSNI(sslSocketSni);
        }
        client = clientBuilder.build();
        return client;
    }

    public boolean ping() {
        if (useClientV2) {
            return pingV2();
        } else {
            return pingV1();
        }
    }

    private boolean pingV1() {
        ClickHouseClient clientPing = ClickHouseClient.builder()
                .options(getDefaultClientOptions())
                .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
                .build();
        LOGGER.debug(String.format("Server [%s] , Timeout [%d]", server, timeout));
        int retryCount = 0;

        while (retryCount < retry) {
            if (clientPing.ping(server, timeout)) {
                LOGGER.info("Ping was successful.");
                clientPing.close();
                return true;
            }
            retryCount++;
            LOGGER.warn(String.format("Ping retry %d out of %d", retryCount, retry));
        }
        LOGGER.error("Unable to ping ClickHouse instance.");
        clientPing.close();
        return false;
    }

    private boolean pingV2() {
        int retryCount = 0;
        while (retryCount < retry) {
            if (client.ping(timeout)) {
                LOGGER.info("Ping was successful.");
                return true;
            }
            retryCount++;
            LOGGER.warn(String.format("Ping retry %d out of %d", retryCount, retry));
        }
        LOGGER.error("Unable to ping ClickHouse instance.");
        return false;
    }

    public String version() {
        if (useClientV2) {
            return versionV2();
        } else {
            return versionV1();
        }
    }

    public String versionV1() {
        try (ClickHouseClient client = ClickHouseClient.builder()
                .options(getDefaultClientOptions())
                .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
                .build();
             ClickHouseResponse response = client.read(server)
                     .query("SELECT VERSION()")
                     .executeAndWait()) {
            return response.firstRecord().getValue(0).asString();
        } catch (ClickHouseException e) {
            LOGGER.error("Exception when trying to retrieve VERSION()", e);
            return null;
        }
    }

    public String versionV2() {
        QuerySettings settings = new QuerySettings().setFormat(ClickHouseFormat.RowBinaryWithNamesAndTypes);
        CompletableFuture<Records> futureRecords = client.queryRecords("SELECT VERSION()", settings);
        try (Records records = futureRecords.get()) {

            for (GenericRecord record : records) {
                return record.getString(1); // string column col3
            }
        } catch (InterruptedException e) {
            LOGGER.error("Exception when trying to retrieve VERSION()", e);
            return null;
        } catch (ExecutionException e) {
            LOGGER.error("Exception when trying to retrieve VERSION()", e);
            return null;
        } catch (Exception e) {
            LOGGER.error("Exception when trying to retrieve VERSION()", e);
            return null;
        }
        return null;
    }

    public ClickHouseResponse queryV1(String query) {
        return queryV1(query, null);
    }

    public ClickHouseResponse queryV1(String query, ClickHouseFormat clickHouseFormat) {
        int retryCount = 0;
        Exception ce = null;
        while (retryCount < retry) {
            try (ClickHouseClient client = ClickHouseClient.builder()
                    .options(getDefaultClientOptions())
                    .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
                    .build();
                 ClickHouseResponse response = client.read(server)
                         .format(clickHouseFormat)
                         .query(query)
                         .executeAndWait()) {
                return response;
            } catch (ClickHouseException e) {
                retryCount++;
                LOGGER.warn(String.format("Query retry %d out of %d", retryCount, retry), e);
                ce = e;
            }
        }
        throw new RuntimeException(ce);
    }

    public Records queryV2(String query) {
        return queryV2(query, new QuerySettings());
    }

    public Records queryV2(String query, QuerySettings settings) {
        int retryCount = 0;
        Exception ce = null;
        while (retryCount < retry) {
            LOGGER.debug("query {} retry {} out of {}", query, retryCount, retry);
            CompletableFuture<Records> futureRecords = client.queryRecords(query, settings);
            try {
                Records records = futureRecords.get();
                return records;
            } catch (ExecutionException e) {
                retryCount++;
                LOGGER.warn(String.format("Query retry %d out of %d", retryCount, retry), e);
                ce = e;
            } catch (InterruptedException e) {
                retryCount++;
                LOGGER.warn(String.format("Query retry %d out of %d", retryCount, retry), e);
                ce = e;
            }
        }
        throw new RuntimeException(ce);
    }

    /**
     * A backslash (or run of backslashes) sitting immediately before a quote character - either
     * one already present in the filter, or the end of the filter (which {@link
     * #buildTableFilterClause(String)} always follows with its own closing quote) - is the only
     * position where a backslash can interact with our quote-doubling below (CR-01).
     */
    private static final Pattern BACKSLASH_RUN_BEFORE_QUOTE_OR_END = Pattern.compile("\\\\+(?='|\\z)");

    /**
     * Builds the {@code match(table, '...')} clause appended to the {@code WHERE} clause (before
     * {@code GROUP BY}) of the {@code system.columns} query that backs {@link
     * #showTables(String)}. Returns the empty string when {@code targetTableFilter} is exactly the
     * empty string (the connector default).
     *
     * <p>Escaping (CR-01, D-19): every single quote is doubled, ClickHouse's own escape for a
     * literal quote inside a string literal. Backslashes are otherwise passed through unchanged so
     * regex character classes like {@code \d+}/{@code \w+} keep matching exactly as {@code match()}
     * interprets them - EXCEPT that a run of backslashes directly before a quote (either one
     * already in the filter, or the quote this method appends to close the literal) is padded to
     * an even length first. ClickHouse decodes string-literal backslash escapes left to right: an
     * even-length run always pairs up and self-cancels, leaving the following quote exactly as our
     * doubling/closing intended, but an odd-length run lets the last backslash escape that quote
     * and break out of the literal - the exact breakout a value like {@code x\' OR 1=1-- } exploits
     * against naive quote-only doubling.
     */
    static String buildTableFilterClause(String targetTableFilter) {
        if (targetTableFilter.isEmpty()) {
            return "";
        }
        Matcher backslashRun = BACKSLASH_RUN_BEFORE_QUOTE_OR_END.matcher(targetTableFilter);
        StringBuilder backslashSafe = new StringBuilder();
        while (backslashRun.find()) {
            String run = backslashRun.group();
            String evenRun = run.length() % 2 == 0 ? run : run + "\\";
            backslashRun.appendReplacement(backslashSafe, Matcher.quoteReplacement(evenRun));
        }
        backslashRun.appendTail(backslashSafe);
        String quoteSafe = backslashSafe.toString().replace("'", "''");
        return String.format(" AND match(table, '%s')", quoteSafe);
    }

    public List<Table.TableDesc> showTables(String database) {
        if (useClientV2) {
            return showTablesV2(database);
        } else {
            return showTablesV1(database);
        }
    }

    public List<Table.TableDesc> showTablesV1(String database) {
        List<Table.TableDesc> tables = new ArrayList<>();
        try (ClickHouseClient client = ClickHouseClient.builder()
                .options(getDefaultClientOptions())
                .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
                .build();
             ClickHouseResponse response = client.read(server)
                     .query(String.format("select database, table, count(*) as col_count from system.columns where database = '%s'", database)
                             + buildTableFilterClause(targetTableFilter)
                             + " group by database, table")
                     .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                     .executeAndWait()) {
            for (ClickHouseRecord r : response.records()) {
                String databaseName = r.getValue(0).asString();
                String tableName = r.getValue(1).asString();
                int colCount = r.getValue(2).asInteger();
                tables.add(new Table.TableDesc(databaseName, tableName, colCount));
            }
        } catch (ClickHouseException e) {
            LOGGER.error("Failed in show tables", e);
        }
        return tables;
    }

    public List<Table.TableDesc> showTablesV2(String database) {
        List<Table.TableDesc> tablesList = new ArrayList<>();
        try (Records records = queryV2(String.format("select database, table, count(*) as col_count from system.columns where database = '%s'", database)
                + buildTableFilterClause(targetTableFilter)
                + " group by database, table")) {
            for (GenericRecord record : records) {
                String databaseName = record.getString(1);
                String tableName = record.getString(2);
                int colCount = record.getInteger(3);
                LOGGER.debug("table name: {}", tableName);
                tablesList.add(new Table.TableDesc(databaseName, tableName, colCount));
            }
        } catch (Exception e) {
            LOGGER.error("Failed in show tables", e);
        }
        return tablesList;
    }

    public Table describeTable(String database, String tableName) {
        if (useClientV2) {
            return describeTableV2(database, tableName);
        } else {
            return describeTableV1(database, tableName);
        }
    }

    /**
     * Returns true when {@code fieldDescriptor} is a structural DESCRIBE sub-column row that
     * should not become its own {@link Column}: a {@code .null}/{@code .size0}/bare {@code .size}
     * suffix (present on every {@code Nullable}/{@code Array} column, regardless of parent type),
     * or any sub-column of a {@code JSON}/{@code Dynamic} parent (whose sub-paths are dynamic and
     * not meaningful as standalone {@link Column}s). Map and Tuple sub-columns are deliberately
     * left un-skipped so {@link Table}'s own parent-routing keeps working.
     *
     * <p>{@code columnsSoFar} is the loop-local map of top-level columns extracted so far in the
     * current DESCRIBE pass, keyed by column name - {@link Table} is immutable and is only
     * constructed once, after the loop, so there is no {@code Table} to query mid-loop. A parent
     * absent from the map (never added, or not yet seen) returns false rather than throwing.
     */
    static boolean shouldSkipDescribeRow(ClickHouseFieldDescriptor fieldDescriptor, Map<String, Column> columnsSoFar) {
        if (!fieldDescriptor.isSubcolumn()) {
            return false;
        }
        String name = fieldDescriptor.getName();
        if (name.endsWith(".null") || name.endsWith(".size0") || name.endsWith(".size")) {
            return true;
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            return false;
        }
        String parentName = name.substring(0, lastDot);
        Column parent = columnsSoFar.get(parentName);
        return parent != null && (parent.getType() == Type.JSON || parent.getType() == Type.DYNAMIC);
    }

    public Table describeTableV1(String database, String tableName) {
        if (tableName.startsWith(".inner"))
            return null;
        String describeQuery = String.format("DESCRIBE TABLE `%s`.`%s`", database, tableName);
        LOGGER.debug(describeQuery);

        try (ClickHouseClient client = ClickHouseClient.builder()
                .options(getDefaultClientOptions())
                .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
                .build();
             ClickHouseResponse response = setReplicaTagHeaderV1(client.read(server)
                     .set("describe_include_subcolumns", true)
                     .format(ClickHouseFormat.JSONEachRow)
                     .query(describeQuery))
                     .executeAndWait()) {

            Set<String> skippedCols = new HashSet<>();
            boolean hasDefaults = false;
            int numColumns = 0;
            List<Column> columns = new ArrayList<>();
            Map<String, Column> columnsByName = new HashMap<>();
            for (ClickHouseRecord r : response.records()) {
                ClickHouseValue v = r.getValue(0);

                ClickHouseFieldDescriptor fieldDescriptor = ClickHouseFieldDescriptor.fromJsonRow(v.asString());
                // Count what system.columns counts: top-level columns, including the ones skipped below
                if (!fieldDescriptor.isSubcolumn()) {
                    numColumns++;
                }

                if (fieldDescriptor.isAlias() || fieldDescriptor.isMaterialized() || fieldDescriptor.isEphemeral()) {
                    LOGGER.debug("Skipping column {} as it is either an alias, materialized view, or ephemeral", fieldDescriptor.getName());
                    skippedCols.add(fieldDescriptor.getName());
                    continue;
                }
                if (fieldDescriptor.isSubcolumn()) {
                    String parentName = fieldDescriptor.getName().substring(0, fieldDescriptor.getName().lastIndexOf("."));
                    if (skippedCols.contains(parentName)) {
                        LOGGER.debug("Skipping subcolumn {} as its parent column is either an alias, materialized view, or ephemeral", fieldDescriptor.getName());
                        skippedCols.add(fieldDescriptor.getName());
                        continue;
                    }
                }

                if (shouldSkipDescribeRow(fieldDescriptor, columnsByName)) {
                    LOGGER.debug("Skipping structural sub-column {}", fieldDescriptor.getName());
                    skippedCols.add(fieldDescriptor.getName());
                    continue;
                }

                if (fieldDescriptor.hasDefault()) {
                    hasDefaults = true;
                }

                Column column = Column.extractColumn(fieldDescriptor);
                //If we run into a rare column we can't handle, just ignore the table and warn the user
                if (column == null) {
                    LOGGER.error("Unable to handle column: {}", fieldDescriptor.getName());
                    return null;
                }
                columns.add(column);
                columnsByName.put(column.getName(), column);
            }

            return new Table(database, tableName, hasDefaults, columns, numColumns);
        } catch (ClickHouseException | JsonProcessingException e) {
            LOGGER.error(String.format("Exception when running describeTable %s", describeQuery), e);
            return null;
        }
    }

    public Table describeTableV2(String database, String tableName) {
        if (tableName.startsWith(".inner"))
            return null;
        String describeQuery = String.format("DESCRIBE TABLE `%s`.`%s`", database, tableName);
        LOGGER.debug(describeQuery);

        try {
            QuerySettings settings = new QuerySettings().setFormat(ClickHouseFormat.JSONEachRow);
            settings.serverSetting("describe_include_subcolumns", "1");
            settings.setDatabase(database);
            setReplicaTagHeaderV2(settings);

            boolean hasDefaults = false;
            int numColumns = 0;
            List<Column> columns = new ArrayList<>();
            Map<String, Column> columnsByName = new HashMap<>();
            try (QueryResponse queryResponse = client.query(describeQuery, settings).get();
                 InputStream describeStream = queryResponse.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(describeStream, StandardCharsets.UTF_8))) {
                String line = null;
                Set<String> skippedCols = new HashSet<>();
                while ((line = br.readLine()) != null) {
                    ClickHouseFieldDescriptor fieldDescriptor = ClickHouseFieldDescriptor.fromJsonRow(line);
                    // Count what system.columns counts: top-level columns, including the ones skipped below
                    if (!fieldDescriptor.isSubcolumn()) {
                        numColumns++;
                    }

                    if (fieldDescriptor.isAlias() || fieldDescriptor.isMaterialized() || fieldDescriptor.isEphemeral()) { // TODO: add subcolumn filter here?
                        LOGGER.debug("Skipping column {} as it is either an alias, materialized view, or ephemeral", fieldDescriptor.getName());
                        skippedCols.add(fieldDescriptor.getName());
                        continue;
                    }
                    if (fieldDescriptor.isSubcolumn()) {
                        String parentName = fieldDescriptor.getName().substring(0, fieldDescriptor.getName().lastIndexOf("."));
                        if (skippedCols.contains(parentName)) {
                            LOGGER.debug("Skipping subcolumn {} as its parent column is either an alias, materialized view, or ephemeral", fieldDescriptor.getName());
                            skippedCols.add(fieldDescriptor.getName());
                            continue;
                        }
                    }

                    if (shouldSkipDescribeRow(fieldDescriptor, columnsByName)) {
                        LOGGER.debug("Skipping structural sub-column {}", fieldDescriptor.getName());
                        skippedCols.add(fieldDescriptor.getName());
                        continue;
                    }

                    if (fieldDescriptor.hasDefault()) {
                        hasDefaults = true;
                    }

                    Column column = Column.extractColumn(fieldDescriptor);
                    //If we run into a rare column we can't handle, just ignore the table and warn the user
                    if (column == null) {
                        LOGGER.error("Unable to handle column: {}", fieldDescriptor.getName());
                        return null;
                    }
                    columns.add(column);
                    columnsByName.put(column.getName(), column);
                }
            }
            return new Table(database, tableName, hasDefaults, columns, numColumns);
        } catch (Exception e) {
            LOGGER.error("describeTableV2 failed", e);
            return null;
        }
    }

    public void alterTableAddColumns(String database, String tableName, List<String> columnDefs, Map<String, String> clickhouseSettings) {
        String addClauses = columnDefs.stream()
                .map(colDef -> "ADD COLUMN IF NOT EXISTS " + colDef)
                .collect(Collectors.joining(", "));
        String sql = String.format("ALTER TABLE `%s`.`%s`%s%s", database, tableName, clusterClause, addClauses);
        LOGGER.info("Executing DDL: {}", sql);
        if (useClientV2) {
            alterTableAddColumnV2(sql, clickhouseSettings);
        } else {
            alterTableAddColumnV1(sql, clickhouseSettings);
        }
    }

    private void alterTableAddColumnV1(String sql, Map<String, String> clickhouseSettings) {
        try (ClickHouseClient client = ClickHouseClient.builder()
                .options(getDefaultClientOptions())
                .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
                .build()) {
            ClickHouseRequest<?> request = client.read(server).query(sql).set("alter_sync", "1");
            for (Map.Entry<String, String> entry : clickhouseSettings.entrySet()) {
                request.set(entry.getKey(), entry.getValue());
            }
            try (ClickHouseResponse response = request.executeAndWait()) {
                // DDL executed; alter_sync=1 waits for the local replica to apply
            }
        } catch (ClickHouseException e) {
            throw new RuntimeException("Failed to execute ALTER TABLE: " + sql, e);
        }
    }

    private void alterTableAddColumnV2(String sql, Map<String, String> clickhouseSettings) {
        QuerySettings settings = new QuerySettings().serverSetting("alter_sync", "1");
        for (Map.Entry<String, String> entry : clickhouseSettings.entrySet()) {
            settings.serverSetting(entry.getKey(), entry.getValue());
        }
        try (QueryResponse response = client.query(sql, settings).get()) {
            // DDL executed; alter_sync=1 waits for the local replica to apply
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute ALTER TABLE: " + sql, e);
        }
    }

    public List<Table> extractTablesMapping(String database, Map<String, Table> cache) {
        List<Table> tableList = new ArrayList<>();
        for (Table.TableDesc tableDesc : showTables(database)) {
            // (Full) Table names are escaped in the cache
            String escapedTableName = tableDesc.getFullName();

            // Read from cache if we already described this table before
            // This means we won't pick up edited table configs until the connector is restarted
            if (cache.containsKey(escapedTableName)) {
                // 2 -> 3
                if (cache.get(escapedTableName).getNumColumns() < tableDesc.getNumColumns()) {
                    LOGGER.info("Table {} has been updated, re-describing", tableDesc.getCleanName());
                } else {
                    // No need to re-describe since no columns have been added
                    continue;
                }
            }
            // A described table counts its own columns, so a DESCRIBE that observed an older schema
            // version than the count query above stays behind the count and is re-described next cycle
            Table tableDescribed = describeTable(database, tableDesc.getCleanName());
            if (tableDescribed != null) {
                // this table will be returned and added to `cache` then will return to this method and
                // rechecked.
                tableList.add(tableDescribed);
            }
        }
        return tableList;
    }

    // thread local without inheritance to pass replica tag to operations
    private final ThreadLocal<String> replicaTag = new ThreadLocal<>();

    public String getReplicaTag() {
        return replicaTag.get();
    }

    /**
     * Pins replica for next calls until unpinReplica() is called.
     * This process is based on setting {@code X-ClickHouse-Replica-Tag} header to randomly generated string.
     * New random string is required to always pin to a new replica.
     * This approach works on for ClickHouse cloud and when enabled.
     * Pinning affects only insert and describe operations to make them hit same replica in case of failure.
     * Do not call it in main path. Code that called pinReplica() must call unpinReplica()
     */
    public void pinReplica() {
        if (!enableReplicaPinning) {
            return;
        }
        try {
            final String chars_en = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int size = 32;
            final StringBuilder tag = new StringBuilder();
            random.ints(size, 0, chars_en.length()).forEach(operand -> tag.append(chars_en.charAt(operand)));
            replicaTag.set(tag.toString());
            LOGGER.debug("Replica pinned by tag {}", replicaTag.get());
        } catch (Exception e) {
            LOGGER.error("Failed to pin replica", e);
        }
    }

    public ClickHouseRequest setReplicaTagHeaderV1(ClickHouseRequest request) {
        String tag = replicaTag.get();
        if (tag != null && request != null) {
            try {
                String customHeaders = (String) request.getConfig().getOption(ClickHouseHttpOption.CUSTOM_HEADERS);
                if (customHeaders == null || customHeaders.trim().isEmpty()) {
                    customHeaders = REPLICA_TAG_HEADER + "=" + tag;
                } else {
                    if (!customHeaders.trim().endsWith(",")) {
                        customHeaders += ",";
                    }
                    customHeaders += REPLICA_TAG_HEADER + "=" + tag;
                }
                request.option(ClickHouseHttpOption.CUSTOM_HEADERS, customHeaders);
            } catch (Exception e) {
                LOGGER.error("Failed to set replica tag header", e);
            }
        }
        return request;
    }

    public void setReplicaTagHeaderV2(QuerySettings settings) {
        String tag = replicaTag.get();
        if (tag != null && settings != null) {
            settings.httpHeader(REPLICA_TAG_HEADER, tag);
        }
    }

    public void setReplicaTagHeaderV2(InsertSettings settings) {
        String tag = replicaTag.get();
        if (tag != null && settings != null) {
            settings.httpHeader(REPLICA_TAG_HEADER, tag);
        }
    }

    /**
     * Unpins replica for next operations.
     * Do not call it in main path.
     */
    public void unpinReplica() {
        replicaTag.remove();
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    public static class ClickHouseClientBuilder {
        private ClickHouseSinkConfig config = null;
        private String hostname = null;
        private int port = -1;
        private String username = "default";
        private String database = "default";
        private String password = "";
        private boolean sslEnabled = false;
        private String jdbcConnectionProperties = "";
        private int timeout = ClickHouseSinkConfig.timeoutSecondsDefault * ClickHouseSinkConfig.MILLI_IN_A_SEC;
        private int retry = ClickHouseSinkConfig.retryCountDefault;

        private ClickHouseProxyType proxyType = null;
        private String proxyHost = null;
        private int proxyPort = -1;
        private boolean useClientV2 = true;
        private String sslSocketSni = "";
        private String clusterClause = "";
        private boolean enableReplicaPinning = false;
        private String targetTableFilter = "";
        private boolean setCompressionMethod = ClickHouseSinkConfig.setCompressionMethodDefault;

        public ClickHouseClientBuilder(String hostname, int port, ClickHouseProxyType proxyType, String proxyHost, int proxyPort) {
            this.hostname = hostname;
            this.port = port;
            this.proxyType = proxyType;
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
        }


        public ClickHouseClientBuilder setUsername(String username) {
            this.username = username;
            return this;
        }

        public ClickHouseClientBuilder setPassword(String password) {
            this.password = password;
            return this;
        }

        public ClickHouseClientBuilder setDatabase(String database) {
            this.database = database;
            return this;
        }

        public ClickHouseClientBuilder sslEnable(boolean sslEnabled) {
            this.sslEnabled = sslEnabled;
            return this;
        }

        public ClickHouseClientBuilder setJdbcConnectionProperties(String jdbcConnectionProperties) {
            this.jdbcConnectionProperties = jdbcConnectionProperties;
            return this;
        }

        public ClickHouseClientBuilder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public ClickHouseClientBuilder setRetry(int retry) {
            this.retry = retry;
            return this;
        }

        public ClickHouseClientBuilder useClientV2(boolean useClientV2) {
            this.useClientV2 = useClientV2;
            return this;
        }

        public ClickHouseClientBuilder setSslSocketSni(String sni) {
            this.sslSocketSni = sni;
            return this;
        }

        public ClickHouseClientBuilder setClusterClause(String clusterName) {
            this.clusterClause = (clusterName == null || clusterName.isEmpty()) ? "" : " ON CLUSTER '" + clusterName + "' ";
            return this;
        }

        public ClickHouseClientBuilder enableReplicaPinning(boolean enableReplicaPinning) {
            this.enableReplicaPinning = enableReplicaPinning;
            return this;
        }

        public ClickHouseClientBuilder setTargetTableFilter(String targetTableFilter) {
            this.targetTableFilter = targetTableFilter;
            return this;
        }

        public ClickHouseClientBuilder setCompressionMethod(boolean setCompressionMethod) {
            this.setCompressionMethod = setCompressionMethod;
            return this;
        }

        public ClickHouseHelperClient build() {
            return new ClickHouseHelperClient(this);
        }

    }
}
