# Traceable OpenTelemetry Java agent

A custom build of OpenTelemetry Java agent.

## Build

```bash
make build
```

## Run

```bash
OTEL_EXPORTER=otlp java -agentlib:jdwp="transport=dt_socket,server=y,suspend=n,address=5000" -javaagent:${HOME}/projects/hypertrace/opentelemetry-java-agent/javaagent/build/libs/traceable-otel-javaagent-0.0.1-all.jar -jar app.jar
```
