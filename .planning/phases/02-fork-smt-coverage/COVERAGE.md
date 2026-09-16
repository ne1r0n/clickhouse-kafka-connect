# API Coverage — Phase 02 Fork SMT Coverage

No external API integration: this phase adds JUnit tests (plus three small in-repo fixes inside
`src/main/java/com/clickhouse/kafka/connect/transforms/`) for four Single Message Transforms that
already ship in the fork. The detector ran over the phase scope and the finished plan bodies and
returned `detected: false`. Occurrences of "Connect API" / `org.apache.kafka:connect-api` refer to
the compile dependency already declared in `build.gradle.kts`, and the ClickHouse container is the
Testcontainers image the Phase 1 harness already starts — neither is a new external service brought
into the project by this phase, so there is no capability surface to enumerate or opt out of.
