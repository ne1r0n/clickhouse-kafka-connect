package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodType;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free guard for BLD-16 (D-13). The connector compiles against connect-api 4.4.0 while the
 * production worker supplies the Connect API at runtime, so a reference to a Kafka class or member
 * that the worker's jars lack fails when the connector is loaded, not when it is built. This test
 * re-derives every {@code org/apache/kafka} class and member the compiled connector references,
 * reading the constant pool of each class file, and fails, naming the entry and the re-bless
 * command, when a reference appears that {@code connect-api-references.txt} does not list. The list
 * is written by {@code .planning/scripts/connect-api-refs.py}, only after the entries are present in
 * the worker's jars, in 4.1.1 and in 4.4.0.
 *
 * <p>It extends nothing, because extending {@code ClickHouseBase} would inherit its {@code
 * clickhouse} tag and drop this class out of {@code testUnit}. To re-bless, run {@code
 * connect-api-refs.py} against the worker jars copied out of the production image and commit the
 * regenerated list together with the evidence that every reference reports zero missing entries.
 */
class ConnectApiReferencesUnitTest {
    private static final String BLESSED_RESOURCE = "/connect-api-references.txt";
    private static final String KAFKA_PREFIX = "org/apache/kafka/";
    private static final String CONNECTOR_PREFIX = "com/clickhouse/kafka/connect/";
    private static final String CLASS_ENTRY = "C ";
    private static final String MEMBER_ENTRY = "M ";
    private static final String CONSTRUCTOR_NAME = "<init>";
    private static final String REBLESS_COMMAND =
            "python3 -I .planning/scripts/connect-api-refs.py --classes build/classes/java/main"
                    + " --compile-classpath <connect-api>:<kafka-clients>"
                    + " --reference worker=<worker connect-api>:<worker kafka-clients>"
                    + " --write src/test/resources/connect-api-references.txt";
    private static final Pattern WELL_FORMED_ENTRY =
            Pattern.compile("C org/apache/kafka/\\S+|M org/apache/kafka/\\S+\\.[^.\\s]+:\\S+");

    private static final String CLASS_ANCHOR = "C org/apache/kafka/connect/sink/SinkTask";
    private static final String CONNECT_METHOD_ANCHOR = "M org/apache/kafka/connect/sink/SinkRecord.topic:()Ljava/lang/String;";
    private static final String INTERFACE_METHOD_ANCHOR = "M org/apache/kafka/connect/header/Headers.size:()I";
    private static final String FIELD_ANCHOR =
            "M org/apache/kafka/connect/sink/SinkTask.context:Lorg/apache/kafka/connect/sink/SinkTaskContext;";
    private static final String INHERITED_ANCHOR =
            "M org/apache/kafka/common/config/AbstractConfig.getBoolean:(Ljava/lang/String;)Ljava/lang/Boolean;";

    private record MemberRef(String owner, String name, String descriptor) {}

    @Test
    @DisplayName("BLD-16: every org.apache.kafka class and member the compiled connector references is in the blessed worker-API list")
    void everyReferencedKafkaClassAndMemberIsInTheBlessedList() throws IOException {
        Set<String> derived = derivedFromConnectorCodeSource();
        Set<String> blessed = blessedReferences(readBlessedLines());

        Optional<String> problem = unlistedProblem(derived, blessed);
        assertTrue(problem.isEmpty(), () -> problem.orElse(""));
        for (String anchor :
                List.of(CLASS_ANCHOR, CONNECT_METHOD_ANCHOR, INTERFACE_METHOD_ANCHOR, FIELD_ANCHOR, INHERITED_ANCHOR)) {
            assertTrue(derived.contains(anchor), "The derivation must find the reference kind anchored by: " + anchor);
        }
    }

    @Test
    @DisplayName("BLD-16: the blessed reference list is well-formed: sorted, unique C and M rows under org/apache/kafka")
    void theBlessedListIsSortedUniqueAndWellFormed() throws IOException {
        List<String> entries = new ArrayList<>(blessedLines(readBlessedLines()));

        assertFalse(entries.isEmpty(), "The blessed list must hold entries");
        for (String entry : entries) {
            assertTrue(WELL_FORMED_ENTRY.matcher(entry).matches(), "Malformed entry in " + BLESSED_RESOURCE + ": " + entry);
        }
        assertEquals(new TreeSet<>(entries).size(), entries.size(), "The blessed list must not repeat an entry");
        assertEquals(new ArrayList<>(new TreeSet<>(entries)), entries, "The blessed list must be sorted");
    }

    @Test
    @DisplayName("fixture: the reference rule names a reference the blessed list does not hold (BLD-16, D-13)")
    void theRuleNamesAReferenceTheListLacks() {
        Optional<String> problem = unlistedProblem(Set.of("C org/apache/kafka/X", "C org/apache/kafka/Y"), Set.of("C org/apache/kafka/X"));
        assertTrue(problem.isPresent(), "A derived entry missing from the list must be reported");
        assertTrue(problem.get().contains("C org/apache/kafka/Y"), "The problem must name the unlisted entry: " + problem.get());
        assertFalse(problem.get().contains("C org/apache/kafka/X\n"), "A listed entry must not be named: " + problem.get());
        assertTrue(problem.get().contains(REBLESS_COMMAND), "The problem must give the re-bless command: " + problem.get());

        assertEquals(
                Optional.empty(),
                unlistedProblem(Set.of("C org/apache/kafka/X"), Set.of("C org/apache/kafka/X", "C org/apache/kafka/Z")),
                "A listed entry that the code no longer references is not a problem");
    }

    static Optional<String> unlistedProblem(Set<String> derived, Set<String> blessed) {
        Set<String> unlisted = new TreeSet<>(derived);
        unlisted.removeAll(blessed);
        if (unlisted.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder message =
                new StringBuilder("The compiled connector references Kafka API entries missing from the blessed list ")
                        .append(BLESSED_RESOURCE)
                        .append(". Each is a class or member the production worker's Connect API may lack:\n");
        unlisted.forEach(entry -> message.append(entry).append('\n'));
        message.append("Check each entry against the worker's connect-api and kafka-clients jars, then re-bless with: ")
                .append(REBLESS_COMMAND);
        return Optional.of(message.toString());
    }

    static Set<String> blessedReferences(List<String> lines) {
        return new TreeSet<>(blessedLines(lines));
    }

    private static List<String> blessedLines(List<String> lines) {
        return lines.stream().map(String::strip).filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();
    }

    private static List<String> readBlessedLines() throws IOException {
        try (InputStream in = ConnectApiReferencesUnitTest.class.getResourceAsStream(BLESSED_RESOURCE)) {
            assertNotNull(in, BLESSED_RESOURCE + " must be on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    /**
     * The test runtime classpath carries the connector either as the main classes directory or, through the
     * test-fixtures dependency on the main project, as its jar; both are read through the same {@link Path} API.
     */
    private static Set<String> derivedFromConnectorCodeSource() throws IOException {
        URL location = ClickHouseSinkConnector.class.getProtectionDomain().getCodeSource().getLocation();
        Path codeSource;
        try {
            codeSource = Path.of(location.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot locate the connector classes at " + location, e);
        }
        if (Files.isDirectory(codeSource)) {
            return derivedReferences(codeSource);
        }
        assertTrue(
                Files.isRegularFile(codeSource) && codeSource.toString().endsWith(".jar"),
                "The connector classes must come from a directory or a jar, but were loaded from " + location);
        try (FileSystem jar = FileSystems.newFileSystem(codeSource)) {
            return derivedReferences(jar.getPath("/"));
        }
    }

    static Set<String> derivedReferences(Path classesDir) throws IOException {
        List<Path> classFiles;
        try (Stream<Path> walk = Files.walk(classesDir.resolve(CONNECTOR_PREFIX))) {
            classFiles = walk.filter(file -> file.toString().endsWith(".class")).sorted().toList();
        }
        assertFalse(classFiles.isEmpty(), "No class files under " + classesDir.resolve(CONNECTOR_PREFIX));

        Set<String> entries = new TreeSet<>();
        Set<MemberRef> connectorRefs = new HashSet<>();
        for (Path classFile : classFiles) {
            try (DataInputStream in = new DataInputStream(Files.newInputStream(classFile))) {
                readConstantPool(in, entries, connectorRefs);
            }
        }
        ClassLoader loader = ClickHouseSinkConnector.class.getClassLoader();
        for (MemberRef ref : connectorRefs) {
            declaringKafkaType(ref, loader)
                    .ifPresent(type -> entries.add(MEMBER_ENTRY + type + "." + ref.name() + ":" + ref.descriptor()));
        }
        return entries;
    }

    private static void readConstantPool(DataInputStream in, Set<String> entries, Set<MemberRef> connectorRefs)
            throws IOException {
        in.readInt();
        in.readUnsignedShort();
        in.readUnsignedShort();
        int count = in.readUnsignedShort();
        String[] utf8 = new String[count];
        int[] tag = new int[count];
        int[] first = new int[count];
        int[] second = new int[count];
        for (int i = 1; i < count; i++) {
            int t = in.readUnsignedByte();
            tag[i] = t;
            switch (t) {
                case 1 -> utf8[i] = in.readUTF();
                case 3, 4 -> in.readInt();
                case 5, 6 -> {
                    in.readLong();
                    i++; // long and double constants take two slots
                }
                case 7, 8, 16, 19, 20 -> first[i] = in.readUnsignedShort();
                case 9, 10, 11, 12, 17, 18 -> {
                    first[i] = in.readUnsignedShort();
                    second[i] = in.readUnsignedShort();
                }
                case 15 -> {
                    in.readUnsignedByte();
                    first[i] = in.readUnsignedShort();
                }
                default -> throw new IOException("Unknown constant pool tag " + t + " at index " + i);
            }
        }
        for (int i = 1; i < count; i++) {
            if (tag[i] == 7) {
                String name = unwrapClassName(utf8[first[i]]);
                if (name.startsWith(KAFKA_PREFIX)) {
                    entries.add(CLASS_ENTRY + name);
                }
            } else if (tag[i] == 9 || tag[i] == 10 || tag[i] == 11) {
                String owner = unwrapClassName(utf8[first[first[i]]]);
                String name = utf8[first[second[i]]];
                String descriptor = utf8[second[second[i]]];
                if (owner.startsWith(KAFKA_PREFIX)) {
                    entries.add(MEMBER_ENTRY + owner + "." + name + ":" + descriptor);
                } else if (owner.startsWith(CONNECTOR_PREFIX)) {
                    connectorRefs.add(new MemberRef(owner, name, descriptor));
                }
            }
        }
    }

    private static String unwrapClassName(String name) {
        String stripped = name.replaceFirst("^\\[+", "");
        if (stripped.length() != name.length() && stripped.startsWith("L") && stripped.endsWith(";")) {
            return stripped.substring(1, stripped.length() - 1);
        }
        return stripped;
    }

    /** Walks the superclass chain, then the interfaces, to the first type declaring the member. */
    private static Optional<String> declaringKafkaType(MemberRef ref, ClassLoader loader) {
        Class<?> owner;
        try {
            owner = Class.forName(ref.owner().replace('/', '.'), false, loader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load the referenced connector class " + ref.owner(), e);
        }
        Set<Class<?>> seen = new HashSet<>();
        Deque<Class<?>> interfaces = new ArrayDeque<>();
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            seen.add(type);
            if (declares(type, ref)) {
                return kafkaTypeName(type);
            }
            interfaces.addAll(List.of(type.getInterfaces()));
        }
        while (!interfaces.isEmpty()) {
            Class<?> type = interfaces.poll();
            if (!seen.add(type)) {
                continue;
            }
            if (declares(type, ref)) {
                return kafkaTypeName(type);
            }
            interfaces.addAll(List.of(type.getInterfaces()));
        }
        return Optional.empty();
    }

    private static Optional<String> kafkaTypeName(Class<?> type) {
        String internalName = type.getName().replace('.', '/');
        return internalName.startsWith(KAFKA_PREFIX) ? Optional.of(internalName) : Optional.empty();
    }

    private static boolean declares(Class<?> type, MemberRef ref) {
        if (CONSTRUCTOR_NAME.equals(ref.name())) {
            return Stream.of(type.getDeclaredConstructors())
                    .anyMatch(c -> methodDescriptor(void.class, c.getParameterTypes()).equals(ref.descriptor()));
        }
        if (ref.descriptor().startsWith("(")) {
            return Stream.of(type.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals(ref.name())
                            && methodDescriptor(m.getReturnType(), m.getParameterTypes()).equals(ref.descriptor()));
        }
        return Stream.of(type.getDeclaredFields())
                .anyMatch(f -> f.getName().equals(ref.name()) && f.getType().descriptorString().equals(ref.descriptor()));
    }

    private static String methodDescriptor(Class<?> returnType, Class<?>[] parameterTypes) {
        return MethodType.methodType(returnType, parameterTypes).toMethodDescriptorString();
    }
}
