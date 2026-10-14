# Phase 18 Deferred Items

## Deferred Items

- ClickHouseWriterNullMessageUnitTest (18-01) inline-mocks ClickHouseHelperClient with Mockito, which crashed the Semeru 17.0.9 (OpenJ9) test JVM natively in retransformClasses when 18-04's capture harness did the same in the full suite. It has passed in every observed run, but carries the same intermittent risk on this machine. Possible follow-up: build the helper client without Mockito, or run the suite on a HotSpot JDK. Found during 18-04 Task 2.
  status: open
