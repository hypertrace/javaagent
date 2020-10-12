[![codecov](https://codecov.io/gh/Traceableai/opentelemetry-javaagent/branch/main/graph/badge.svg?token=MM5BVNGPKE)](https://codecov.io/gh/Traceableai/opentelemetry-javaagent)
[![CircleCI](https://circleci.com/gh/Traceableai/opentelemetry-javaagent.svg?style=svg)](https://circleci.com/gh/Traceableai/opentelemetry-javaagent)

# Hypertrace OpenTelemetry Java agent

Hypertrace distribution of [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation).

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

The final artifact is in `javaagent/build/libs/hypertrace-agent-<version>-all.jar`

## Run

```bash
OTEL_EXPORTER=zipkin java -javaagent:javaagent/build/libs/hypertrace-agent-0.0.1-all.jar -jar app.jar
```

### Run with Traceable.ai config

The precedence order of different configuration 
1. OpenTelemetry Agent's trace config file `OTEL_TRACE_CONFIG`/`otel.trace.config`
2. Traceable.ai configuration file
3. OpenTelemetry system properties and env variables

Follows and example of using Traceable.ai config file (`example-config.json`):

```bash
java -javaagent:javaagent/build/libs/hypertrace-agent-0.0.1-all.jar=traceableConfigFile=example-config.json -jar app.jar
```

Supported agent arguments:

* `traceableConfigFile` - path to traceable config file
* `traceableServiceName` - service name of the monitored process

### Disable request/response body capture

Request and response body capture can be disabled by `-Dotel.integration.body.enabled=false` or
`-Dotel.integration.<instrumentation>-body.enabled=false`.

## Test

Tests use docker via Testcontainers.org.

When running tests from IDE set `SMOKETEST_JAVAAGENT_PATH` env variable.

```bash
make test
```
