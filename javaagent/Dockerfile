# This is a dockerfile that contains the Hypertrace javaagent.
# This image can be used to get the javaagent into app images or
# as Kubernetes init container to copy the javaagent into the volume mounted into the
# application container.

FROM hypertrace/java:11

LABEL maintainer="Hypertrace 'https://www.hypertrace.org/'"

ENV JAVAAGENT=/opt/hypertrace/hypertrace-agent-all.jar
COPY build/libs/hypertrace-agent-*-all.jar ${JAVAAGENT}

# The following binaries are used by sidecar injector
# This statement tests if they exists in the image
RUN which base64

# env vars are not interpreted
CMD ["/opt/hypertrace/hypertrace-agent-all.jar"]
