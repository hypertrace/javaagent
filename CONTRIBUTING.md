# Contributing

## Upgrade OpenTelemetry auto instrumentation

To upgrade OpenTelemetry auto instrumentation (agent) follow these steps:

1. Upgrade OpenTelemetry API and agent versions in [build.gradle.kts](./build.gradle.kts). 
   The OpenTelemetry API version has to match the version used by the agent.
2. Make sure shadow replace configuration from [build.gradle.kts](./build.gradle.kts) and [./instrumentation/build.gradle.kts](./instrumentation/build.gradle.kts)
   is aligned with https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/5ef915879162bcccd0f3727d25bb735816263188/javaagent/javaagent.gradle#L83-L95 
   and https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/5ef915879162bcccd0f3727d25bb735816263188/instrumentation/instrumentation.gradle#L66-L76
3. Make sure (./buildSrc) is synced with https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/master/buildSrc. 
   It should be one to one copy.
4. Make sure Hypertrace instrumentation names match with OpenTelemetry instrumentation names.
