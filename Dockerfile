FROM alpine:3.12

COPY javaagent/build/libs/*-all.jar /mnt/hypertrace/hypertrace-agent-all.jar

RUN mkdir -p /mnt/hypertrace

VOLUME /mnt/hypertrace

ENTRYPOINT ["cp", "-v", "hypertrace-agent-all.jar", "/mnt/hypertrace"]
