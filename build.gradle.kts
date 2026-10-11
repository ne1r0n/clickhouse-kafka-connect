import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val defaultJdkVersion = 17
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    idea
    `java-library`
    `jvm-test-suite`
    `maven-publish`
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.shadow)
    alias(libs.plugins.protobuf)
    id("com.github.ben-manes.versions") version "0.53.0"
    id("java-test-fixtures")
}

// Configure Gradle Versions Plugin to reject non-stable versions in dependencyUpdates
tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates").configure {
    // Reject all non-stable versions
    rejectVersionIf {
        val candidate = candidate.version
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { candidate.uppercase().contains(it) }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(candidate)
        !isStable
    }
}

group = "com.clickhouse.kafka"
version = file("VERSION").readText().trim()
description = "The official ClickHouse Apache Kafka Connect Connector."

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

val clickhouseDependencies: Configuration by configurations.creating

// The ClickHouse client-v2 depends on the maintained `at.yawk.lz4:lz4-java` fork, while
// kafka-clients depends on the original `org.lz4:lz4-java`. Both declare the same
// `org.lz4:lz4-java` capability, so Gradle needs to be told which one to keep. The fork is a
// drop-in replacement (same `net.jpountz.lz4` package) and newer, so we select it everywhere.
configurations.all {
    resolutionStrategy.capabilitiesResolution.withCapability("org.lz4:lz4-java") {
        val fork = candidates.firstOrNull {
            (it.id as? ModuleComponentIdentifier)?.group == "at.yawk.lz4"
        }
        if (fork != null) {
            select(fork)
            because("client-v2 uses the maintained at.yawk.lz4 fork, a drop-in replacement for org.lz4:lz4-java")
        }
    }
}

dependencies {
    // Exposed as `api` because they leak into the connector's public signatures
    // (e.g. ClickHouseHelperClient returns ClickHouseResponse/Records, ClickHouseSinkTask
    // extends SinkTask), so consumers such as the `benchmark` project resolve them
    // transitively instead of pinning their own, possibly divergent, versions.
    api(libs.kafka.connect.api)
    api(libs.clickhouse.client)
    api(libs.clickhouse.http.client)
    api(libs.clickhouse.data)
    api(libs.clickhouse.client.v2)
    implementation(libs.lz4.java)
    implementation(libs.gson)
    implementation(libs.httpclient5)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)

    // FORK OVERRIDE (D-16): no upstream catalog entry for this coordinate.
    implementation("org.slf4j:slf4j-reload4j:2.0.17")

    /*
        Will in side the Confluent Archive
     */
    clickhouseDependencies(libs.httpclient5)
    clickhouseDependencies(libs.clickhouse.client)
    clickhouseDependencies(libs.clickhouse.client.v2)
    clickhouseDependencies(libs.lz4.java)
    clickhouseDependencies(libs.clickhouse.http.client)
    clickhouseDependencies(libs.gson)
    clickhouseDependencies(libs.jackson.core)
    clickhouseDependencies(libs.jackson.databind)
    clickhouseDependencies(libs.jackson.annotations)

    // Unit Tests
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.runner)
    testImplementation(libs.apiguardian.api)
    testImplementation(libs.hamcrest)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.clickhouse)
    testImplementation(libs.testcontainers.toxiproxy)

    // Schema Registry client for testing
    testImplementation(libs.kafka.schema.registry.client)
    testImplementation(libs.kafka.schema.registry)
    testImplementation(libs.kafka.schema.serializer)

    // Test Fixtures Dependencies
    // Test Fixtures is used to extract helpers into a separate jar and publish locally to use in other test projects
    // like JMH Benchmark
    // Protobuf dependencies
    testFixturesApi(libs.protobuf.java)
    testFixturesApi(libs.kafka.protobuf.serializer)
    testFixturesApi(libs.kafka.connect.protobuf.converter)
    testFixturesApi(libs.kafka.connect.avro.converter)
    testFixturesApi(libs.kafka.avro.serializer)

    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.junit.platform.runner)
    testFixturesImplementation(libs.kafka.connect.api)
    testFixturesImplementation(libs.commons.lang3)
    testFixturesImplementation(libs.clickhouse.client)
    testFixturesImplementation(libs.clickhouse.http.client)
    testFixturesImplementation(libs.clickhouse.data)
    testFixturesImplementation(libs.clickhouse.client.v2)
    testFixturesImplementation(libs.gson)
    testFixturesImplementation(libs.json)
    testFixturesImplementation(libs.testcontainers)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(project(":"))
                implementation(project.dependencies.testFixtures(project(":")))

                implementation(libs.testcontainers.clickhouse)
                implementation(libs.testcontainers.kafka)
                implementation(libs.testcontainers.toxiproxy)
                implementation(libs.okhttp)
                implementation(libs.json)
                implementation(libs.httpclient5.test)
                implementation(libs.clickhouse.client)
                implementation(libs.clickhouse.client.v2)
                implementation(libs.clickhouse.http.client)
                implementation(libs.slf4j.simple)
                implementation(libs.kafka.connect.api)
            }
            targets {
                all {
                    testTask.configure {
                        description = "Runs the integration tests"
                        group = "verification"
                        shouldRunAfter(test)
                        dependsOn("prepareConfluentArchive")
                        outputs.upToDateWhen { false }

                        val systemProps = System.getProperties().entries.associate { it.key.toString() to it.value }
                        systemProperties(systemProps)
                    }
                }
            }
        }
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
}

tasks.withType<Test> {
    if (System.getenv("CLICKHOUSE_CLUSTER_NAME") != null) {
        maxParallelForks = 1
    } else {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    }
    if (name != "testUnit") {
        tasks.getByName("check").dependsOn(this)
    }
    systemProperty("file.encoding", "windows-1252") // run tests with different encoding

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }

    val javaVersion: Int = (project.findProperty("javaVersion") as String? ?: defaultJdkVersion.toString()).toInt()
    logger.info("Running tests using JDK$javaVersion")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    })

    systemProperties(mapOf("com.clickhouse.test.uri" to System.getProperty("com.clickhouse.test.uri", "")))

    addTestListener(object : TestListener {
        override fun beforeTest(testDescriptor: TestDescriptor?) {}
        override fun beforeSuite(suite: TestDescriptor?) {}
        override fun afterTest(testDescriptor: TestDescriptor?, result: TestResult?) {}
        override fun afterSuite(d: TestDescriptor?, r: TestResult?) {
            if (d != null && r != null && d.parent == null) {
                val resultsSummary = """Tests summary:
                    | ${r.testCount} tests,
                    | ${r.successfulTestCount} succeeded,
                    | ${r.failedTestCount} failed,
                    | ${r.skippedTestCount} skipped""".trimMargin().replace("\n", "")

                val border = "=".repeat(resultsSummary.length)
                logger.lifecycle("\n$border")
                logger.lifecycle("Test result: ${r.resultType}")
                logger.lifecycle(resultsSummary)
                logger.lifecycle("${border}\n")
            }
        }
    })
}

// CI reads the fork count from this task instead of recomputing the formula in shell (CI-14, 14-REVIEW WR-02).
tasks.register("reportTestForks") {
    group = "verification"
    description = "Prints the effective maxParallelForks of the test task, one line (read by CI)"
    val forks = tasks.named<Test>("test").map { it.maxParallelForks }
    doLast {
        println(forks.get())
    }
}

// INFRA-04, D-12: runs every test not tagged @Tag("clickhouse"); no Docker required.
// Inherits fork-count parallelism, windows-1252 file.encoding, test logging, the Java 17
// launcher, and the summary listener from the withType<Test> block above.
tasks.register<Test>("testUnit") {
    group = "verification"
    description = "Runs every test not tagged clickhouse; no Docker required"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        excludeTags("clickhouse")
    }
    extensions.configure<JacocoTaskExtension> { isEnabled = false }
}

jacoco {
    toolVersion = "0.8.15"
}

// D-13: hand-picked, named include list of the fork-changed classes (COV-02).
// Exact class names, not bare prefix globs, so neighbours such as
// TableMappingRefresher, KeyToValue, ExtractTopic, or ClickHouseFieldDescriptor
// can never be pulled in.
val forkChangedClasses = listOf(
    "com/clickhouse/kafka/connect/transforms/TableOperationFilter.class",
    "com/clickhouse/kafka/connect/transforms/TableOperationFilter\$*.class",
    "com/clickhouse/kafka/connect/transforms/HeadersToValue.class",
    "com/clickhouse/kafka/connect/transforms/HeadersToValue\$*.class",
    "com/clickhouse/kafka/connect/transforms/HeaderToValue.class",
    "com/clickhouse/kafka/connect/transforms/HeaderToValue\$*.class",
    "com/clickhouse/kafka/connect/transforms/HeaderToValueConfig.class",
    "com/clickhouse/kafka/connect/transforms/HeaderToValueConfig\$*.class",
    "com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.class",
    "com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson\$*.class",
    "com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.class",
    "com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient\$*.class",
    "com/clickhouse/kafka/connect/sink/db/mapping/Column.class",
    "com/clickhouse/kafka/connect/sink/db/mapping/Column\$*.class",
    "com/clickhouse/kafka/connect/sink/db/mapping/Type.class",
    "com/clickhouse/kafka/connect/sink/db/mapping/Type\$*.class",
    "com/clickhouse/kafka/connect/sink/db/mapping/Table.class",
    "com/clickhouse/kafka/connect/sink/db/mapping/Table\$*.class",
    "com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.class",
    "com/clickhouse/kafka/connect/sink/db/ClickHouseWriter\$*.class",
    "com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.class",
    "com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig\$*.class",
    "com/clickhouse/kafka/connect/sink/ProxySinkTask.class",
    "com/clickhouse/kafka/connect/sink/ProxySinkTask\$*.class",
    "com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.class",
    "com/clickhouse/kafka/connect/sink/ClickHouseSinkTask\$*.class",
    "com/clickhouse/kafka/connect/ClickHouseSinkConnector.class",
    "com/clickhouse/kafka/connect/ClickHouseSinkConnector\$*.class",
    "com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.class",
    "com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils\$*.class"
)

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// D-14: fork-changed-class-only coverage view, reading the same test exec data.
tasks.register<JacocoReport>("jacocoForkReport") {
    group = "verification"
    description = "Generates a JaCoCo coverage report scoped to the fork-changed classes (D-13)"
    dependsOn(tasks.named("test"))
    executionData(tasks.named<Test>("test").get())
    sourceDirectories.setFrom(sourceSets.main.get().allJava.srcDirs)
    classDirectories.setFrom(files(sourceSets.main.get().output.classesDirs.map { fileTree(it) { include(forkChangedClasses) } }))
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/fork/jacocoForkReport.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/fork/html"))
    }
}

// D-16: both reports hang off test only; never wired to check or a coverage-verification rule.
tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"), tasks.named("jacocoForkReport"))
}

// D-16: JaCoCo agent runs on test only.
tasks.named<Test>("integrationTest") {
    extensions.configure<JacocoTaskExtension> { isEnabled = false }
}

/*
 * ShadowJar
 */
tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Title"] = "ClickHouse-Kafka-Connect"
        attributes["Implementation-Version"] = project.version.toString()
    }
}
tasks.register<ShadowJar>("confluentJar") {
    archiveClassifier.set("confluent")
    from(clickhouseDependencies, sourceSets.main.get().output)
}

// Confluent Archive
val releaseDate by extra(DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDateTime.now()))
val archiveFilename = "clickhouse-kafka-connect"
tasks.register<Copy>("prepareConfluentArchive") {
    group = "Confluent"
    description = "Prepares the Confluent Archive ready for the hub"
    dependsOn("confluentJar")

    val baseDir = "$archiveFilename-${project.version}"
    destinationDir = layout.buildDirectory.dir("confluentArchive/$baseDir").get().asFile

    from("config/archive/manifest.json") {
        expand(project.properties)
    }

    from("config/archive/assets") {
        into("assets")
    }

    from(layout.buildDirectory.dir("libs")) {
        include("${project.name}-${project.version}-confluent.jar")
        into("lib")
    }

    from(".") {
        include("README.md", "LICENSE")
        into("doc")
    }
}

tasks.register<Zip>("createConfluentArchive") {
    group = "Confluent"
    description = "Creates the Confluent Archive zipfile to be uploaded to the Confluent Hub"
    dependsOn("prepareConfluentArchive")
    from(layout.buildDirectory.dir("confluentArchive"))
    archiveBaseName.set("")
    archiveAppendix.set(archiveFilename)
    archiveVersion.set(project.version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("confluent"))
}

tasks.register<Exec>("runClickHouseVersionClientMatrix") {
    group = "verification"
    description = "Runs tests for ClickHouse and client version matrix and writes logs to build/"
    commandLine("bash", "${project.rootDir}/scripts/run-ch-version-client-matrix.sh")

    doFirst {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        val outputDir = layout.buildDirectory.dir("matrix-test-results/$timestamp").get().asFile
        outputDir.mkdirs()

        val clickhouseVersions = (project.findProperty("matrixClickHouseVersions") as String?) ?: "24.8,25.2,25.8,26.3,latest"
        val clientVersions = (project.findProperty("matrixClientVersions") as String?) ?: "V1,V2"
        val testTask = (project.findProperty("matrixTestTask") as String?) ?: "test"
        val continueOnFailure = (project.findProperty("matrixContinueOnFailure") as String?) ?: "true"
        val matrixGradleArgs = (project.findProperty("matrixGradleArgs") as String?) ?: ""

        environment("MATRIX_OUTPUT_DIR", outputDir.absolutePath)
        environment("CH_VERSIONS", clickhouseVersions)
        environment("CLIENT_VERSIONS", clientVersions)
        environment("TEST_TASK", testTask)
        environment("CONTINUE_ON_FAILURE", continueOnFailure)
        environment("MATRIX_GRADLE_ARGS", matrixGradleArgs)

        logger.lifecycle("Matrix test logs will be written to: ${outputDir.absolutePath}")
    }
}

var generateVersionFile = tasks.register<DefaultTask>("generateVersionFile") {
    val outputDir = "generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/";
    outputs.dir(layout.buildDirectory.dir(outputDir))
    doFirst {
        val outputDirFile = layout.buildDirectory.dir(outputDir).get().asFile
        outputDirFile.mkdirs() // Ensure the directory exists

        val outputFile = File(outputDirFile, "Version.java")
        val versionContent = """
package com.clickhouse.kafka.connect.sink;

public final class Version {
    public static final String ARTIFACT_VERSION = "${project.version}";

    private Version() {
        // Prevent instantiation
    }
}
""".trimIndent()

        outputFile.writeText(versionContent)
    }
}
tasks.named("compileJava") {
    dependsOn("generateVersionFile")
}

sourceSets {
    main {
        java {
            srcDir(generateVersionFile)
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.libprotobuf.get()}"
    }
}

/*
 * Publishing
 *
 * The connector is not published to Maven Central. Publish it to the local Maven
 * repository (~/.m2) so standalone consumers — such as the JMH `benchmark` project —
 * can depend on it by version. Because the `java-test-fixtures` plugin is applied,
 * the `java` component also carries the test-fixtures variant (and Gradle Module
 * Metadata), letting consumers resolve `testFixtures(...)` from mavenLocal().
 *
 *   ./gradlew publishToMavenLocal
 */
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

spotless {
    java {
        importOrder()
        removeUnusedImports()
        // googleJavaFormat()
    }
}

tasks.register("removeUnusedImports") {
    group = "formatting"
    description = "Removes unused imports from all Java files in the project"
    dependsOn("spotlessApply")
}
