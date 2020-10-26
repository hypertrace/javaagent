[![CircleCI](https://circleci.com/gh/hypertrace/opentelemetry-javaagent.svg?style=svg&circle-token=b562d40d95cc5906f445004c4a96b666250d260b)](https://circleci.com/gh/hypertrace/opentelemetry-javaagent)

# Hypertrace OpenTelemetry Java agent

Hypertrace distribution of [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation).

This agent supports [these frameworks](https://github.com/open-telemetry/opentelemetry-java-instrumentation#supported-java-libraries-and-frameworks)
and adds following capabilities:
* capture request and response headers
* capture request and response bodies
* execution blocking based on Open Policy Agent

List of supported frameworks with additional capabilities:
| Library/Framework                                                                                      | Versions        |
|--------------------------------------------------------------------------------------------------------|-----------------|
| [Servlet](https://javaee.github.io/javaee-spec/javadocs/javax/servlet/package-summary.html)            | 2.3+            |
| [Spark Web Framework](https://github.com/perwendel/spark)                                              | 2.3+            |


## Build

```bash
make build
```

The final artifact is in `javaagent/build/libs/hypertrace-agent-<version>-all.jar`

## Run

```bash
OTEL_EXPORTER=zipkin java -javaagent:javaagent/build/libs/hypertrace-agent-0.0.1-all.jar -jar app.jar
```

The configuration precedence order 
1. OpenTelemetry Agent's trace config file `OTEL_TRACE_CONFIG`/`otel.trace.config`
3. OpenTelemetry system properties and env variables

Hypertrace body/headers capture can be disabled by:
* `HYPERTRACE_INTEGRATION_ALL_ENABLED` - disables capture for all instrumentations
* `HYPERTRACE_INTEGRATION_<integration>_ENABLED` - disables capture for a specified instrumentation e.g. `servlet`, `servlet-3`

### Disable request/response body capture

Request and response body capture can be disabled by `-Dotel.integration.body.enabled=false` or
`-Dotel.integration.<instrumentation>-body.enabled=false`.

## Test

Tests use docker via Testcontainers.org.

When running tests from IDE set `SMOKETEST_JAVAAGENT_PATH` env variable.

```bash
make test
```
