[![CircleCI](https://circleci.com/gh/hypertrace/javaagent.svg?style=svg&circle-token=b562d40d95cc5906f445004c4a96b666250d260b)](https://circleci.com/gh/hypertrace/javaagent)

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
| [gRPC](https://github.com/grpc/grpc-java)                                                              | 1.5+            |
| [OkHttp](https://github.com/square/okhttp/)                                                            | 3.0+            |


## Build

```bash
make build
```

The final artifact is in `javaagent/build/libs/hypertrace-agent-<version>-all.jar`

## Run & Configure

```bash
HT_EXPORTING_ADDRESS=http://localhost:9411/api/v2/spans java -javaagent:javaagent/build/libs/hypertrace-agent-<version>-all.jar -jar app.jar
```

By default the agent uses Zipkin exporter.

The configuration precedence order 
1. OpenTelemetry Agent's trace config file `OTEL_TRACE_CONFIG`/`otel.trace.config`
2. OpenTelemetry system properties and env variables
3. Hypertrace configuration with the following precedence order:
   1. system properties 
   2. environment variables, TODO add link to agent-config repo
   3. [configuration file](./example-config.yaml), specified `HT_CONFIG_FILE=example-config.yaml`

Hypertrace body/headers capture can be disabled by:
* `HT_DATA_CAPTURE_HTTP_BODY_REQUEST` - disables additional data capture for all instrumentations
* `HYPERTRACE_INTEGRATION_<integration>_ENABLED` - disables capture for a specified instrumentation e.g. `servlet`, `servlet-3`

## Test

Tests use docker via Testcontainers.org.

When running tests from IDE set `SMOKETEST_JAVAAGENT_PATH` env variable.

```bash
make test
```
