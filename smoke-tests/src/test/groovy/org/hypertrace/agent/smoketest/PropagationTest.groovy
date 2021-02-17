/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hypertrace.agent.smoketest

import static java.util.stream.Collectors.toSet

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import okhttp3.Request

abstract class PropagationTest extends SmokeTest {

  @Override
  protected String getTargetImage(String jdk, String serverVersion) {
    "ghcr.io/open-telemetry/java-test-containers:smoke-springboot-jdk$jdk-20210129.520311771"
  }

  def "Should propagate test"() {
    setup:
    startTarget(11)
    String url = "http://localhost:${target.getMappedPort(8080)}/front"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    Collection<ExportTraceServiceRequest> traces = waitForTraces()
    def traceIds = getSpanStream(traces)
      .map({ bytesToHex(it.getTraceId().toByteArray()) })
      .collect(toSet())

    then:
    traceIds.size() == 1

    def traceId = traceIds.first()

    response.body().string() == "${traceId};${traceId}"

    cleanup:
    stopTarget()

  }

}

class DefaultPropagationTest extends PropagationTest {
}

class W3CPropagationTest extends PropagationTest {
  @Override
  protected Map<String, String> getExtraEnv() {
    return ["otel.propagators": "tracecontext"]
  }
}

class B3PropagationTest extends PropagationTest {
  @Override
  protected Map<String, String> getExtraEnv() {
    return ["otel.propagators": "b3"]
  }
}

class B3MultiPropagationTest extends PropagationTest {
  @Override
  protected Map<String, String> getExtraEnv() {
    return ["otel.propagators": "b3multi"]
  }
}

class JaegerPropagationTest extends PropagationTest {
  @Override
  protected Map<String, String> getExtraEnv() {
    return ["otel.propagators": "jaeger"]
  }
}

class OtTracerPropagationTest extends SmokeTest {
  @Override
  protected String getTargetImage(String jdk, String serverVersion) {
    "ghcr.io/open-telemetry/java-test-containers:smoke-springboot-jdk$jdk-20210129.520311771"
  }

  // OtTracer only propagates lower half of trace ID so we have to mangle the trace IDs similar to
  // the Lightstep backend.
  def "Should propagate test"() {
    setup:
    startTarget(11)
    String url = "http://localhost:${target.getMappedPort(8080)}/front"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    Collection<ExportTraceServiceRequest> traces = waitForTraces()
    def traceIds = getSpanStream(traces)
      .map({ bytesToHex(it.getTraceId().toByteArray()).substring(16) })
      .collect(toSet())

    then:
    traceIds.size() == 1

    def traceId = traceIds.first()

    response.body().string().matches(/[0-9a-f]{16}${traceId};[0]{16}${traceId}/)

    cleanup:
    stopTarget()
  }

  @Override
  protected Map<String, String> getExtraEnv() {
    return ["otel.propagators": "ottracer"]
  }
}

class XRayPropagationTest extends PropagationTest {
  @Override
  protected Map<String, String> getExtraEnv() {
    return ["otel.propagators": "xray"]
  }
}
