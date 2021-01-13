[![CircleCI](https://circleci.com/gh/hypertrace/javaagent.svg?style=svg&circle-token=b562d40d95cc5906f445004c4a96b666250d260b)](https://circleci.com/gh/hypertrace/javaagent)

# Hypertrace OpenTelemetry Java agent

Hypertrace distribution of [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation).

This agent supports [these frameworks](https://github.com/open-telemetry/opentelemetry-java-instrumentation#supported-java-libraries-and-frameworks)
and adds following capabilities:
* capture request and response headers
* capture request and response bodies
* server request headers/bodies evaluation in agent filter that can result in request blocking.
    The filter implementation will be pluggable.

List of supported frameworks with additional capabilities:
| Library/Framework                                                                                      | Versions        |
|--------------------------------------------------------------------------------------------------------|-----------------|
| [Apache HttpAsyncClient](https://hc.apache.org/index.html)                                             | 4.1+            |
| [Apache HttpClient](https://hc.apache.org/index.html)                                                  | 4.0+            |
| [gRPC](https://github.com/grpc/grpc-java)                                                              | 1.5+            |
| [JAX-RS Client](https://javaee.github.io/javaee-spec/javadocs/javax/ws/rs/client/package-summary.html) | 2.0+            |
| [Micronaut](https://micronaut.io/)  (basic support via Netty)                                          | 1.0+            |
| [Netty](https://github.com/netty/netty)                                                                | 4.0+            |
| [OkHttp](https://github.com/square/okhttp/)                                                            | 3.0+            |
| [Servlet](https://javaee.github.io/javaee-spec/javadocs/javax/servlet/package-summary.html)            | 2.3+            |
| [Spark Web Framework](https://github.com/perwendel/spark)                                              | 2.3+            |
| [Spring Webflux](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/reactive/package-summary.html)       | 5.0+            |
| [Vert.x](https://vertx.io)                                                                             | 3.0+            |

### Adding custom filter implementation

Custom filter implementations can be added via `FilterProvider` SPI (Java service loader).
The providers can be disabled at startup via `ht.filter.provider.<provider-class-name>.disabled=true`.

## Build

```bash
make build
```

The final artifact is in `javaagent/build/libs/hypertrace-agent-<version>-all.jar`

## Run & Configure

Download the [latest version](https://github.com/hypertrace/javaagent/releases/latest/download/hypertrace-agent-all.jar).

```bash
HT_EXPORTING_ENDPOINT=http://localhost:9411/api/v2/spans java -javaagent:javaagent/build/libs/hypertrace-agent-<version>-all.jar -jar app.jar
```

By default the agent uses Zipkin exporter.

The configuration precedence order 
1. OpenTelemetry Agent's trace config file `OTEL_TRACE_CONFIG`/`otel.trace.config`
2. OpenTelemetry system properties and env variables
3. Hypertrace configuration with the following precedence order:
   1. system properties 
   2. environment variables, TODO add link to agent-config repo
   3. [configuration file](./example-config.yaml), specified `HT_CONFIG_FILE=example-config.yaml`

### Disable instrumentation at startup

Instrumentations can be disabled by `-Dotel.instrumentation.<instrumentation-name>.enabled=false`.

The following instrumentation names disable only Hypertrace instrumentations, not core OpenTelemetry:

* `ht` - all Hypertrace instrumentations
* `servlet-ht` - Servlet, Spark Web
* `okhttp-ht` - Okhttp
* `grpc-ht` - gRPC

The Hypertrace instrumentations use also the core OpenTelemetry instrumentation names so for example
`-Dotel.instrumentation.servlet.enabled=false` disables all servlet instrumentations including core
OpenTelemetry and Hypertrace.

## Test

Tests use docker via Testcontainers.org.

When running tests from IDE set `SMOKETEST_JAVAAGENT_PATH` env variable.

```bash
make test
```
