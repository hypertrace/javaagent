[![codecov](https://codecov.io/gh/Traceableai/goagent/branch/master/graph/badge.svg?token=MM5BVNGPKE)](https://codecov.io/gh/Traceableai/opentelemetry-javaagent)
[![CircleCI](https://circleci.com/gh/Traceableai/opentelemetry-javaagent.svg?style=svg)](https://circleci.com/gh/Traceableai/opentelemetry-javaagent)

# Traceable OpenTelemetry Java agent

Traceable distribution of [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation).

In addition to the upstream this project adds these capabilities:
* capture request and response headers
* capture request and response bodies
* execution blocking based on Open Policy Agent

List of supported frameworks with additional capabilities:
| Library/Framework                                                                                      | Versions        |
|--------------------------------------------------------------------------------------------------------|-----------------|
| [Servlet](https://javaee.github.io/javaee-spec/javadocs/javax/servlet/package-summary.html)            | 3.0+            |


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
