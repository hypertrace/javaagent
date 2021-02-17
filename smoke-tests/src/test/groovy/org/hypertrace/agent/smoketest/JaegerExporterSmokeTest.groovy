/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hypertrace.agent.smoketest

import static java.util.stream.Collectors.toSet

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import java.util.jar.Attributes
import java.util.jar.JarFile
import okhttp3.Request

class JaegerExporterSmokeTest extends SmokeTest {

  protected String getTargetImage(String jdk, String serverVersion) {
    "ghcr.io/open-telemetry/java-test-containers:smoke-springboot-jdk$jdk-20210129.520311771"
  }

  @Override
  protected Map<String, String> getExtraEnv() {
    return [
      "OTEL_TRACES_EXPORTER"          : "jaeger",
      "OTEL_EXPORTER_JAEGER_ENDPOINT" : "collector:14250"
    ]
  }

  def "spring boot smoke test with jaeger grpc"() {
    setup:
    startTarget(11)

    String url = "http://localhost:${target.getMappedPort(8080)}/greeting"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    Collection<ExportTraceServiceRequest> traces = waitForTraces()

    then:
    response.body().string() == "Hi!"
    countSpansByName(traces, '/greeting') == 1
    countSpansByName(traces, 'WebController.greeting') == 1
    countSpansByName(traces, 'WebController.withSpan') == 1

    cleanup:
    stopTarget()

  }

}
