# Traceable OpenTelemetry Java agent

Traceable distribution of [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation).

In addition to the upstream this project adds these capabilities:
* capture request and response headers
* capture request and response bodies
* execution blocking based on Open Policy Agent

## Build

```bash
make build
```

## Run

```bash
OTEL_EXPORTER=otlp java -agentlib:jdwp="transport=dt_socket,server=y,suspend=n,address=5000" -javaagent:${HOME}/projects/hypertrace/opentelemetry-java-agent/javaagent/build/libs/traceable-otel-javaagent-0.0.1-all.jar -jar app.jar
```

## Test

Tests use docker via Testcontainers.org.

When running tests from IDE set `SMOKETEST_JAVAAGENT_PATH` env variable.

```bash
make test
```
