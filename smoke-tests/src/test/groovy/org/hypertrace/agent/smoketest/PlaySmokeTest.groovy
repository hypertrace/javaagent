/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hypertrace.agent.smoketest

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import okhttp3.Request

class PlaySmokeTest extends SmokeTest {

  protected String getTargetImage(String jdk, String serverVersion) {
    "ghcr.io/open-telemetry/java-test-containers:smoke-play-jdk$jdk-20201128.1734635"
  }

  def "play smoke test on JDK #jdk"(int jdk) {
    setup:
    startTarget(jdk)
    String url = "http://localhost:${target.getMappedPort(8080)}/welcome?id=1"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    Collection<ExportTraceServiceRequest> traces = waitForTraces()

    then:
    response.body().string() == "Welcome 1."

    // Play produces one Internal span and akka produces a server span
    countSpansByName(traces, '/welcome') == 1 // play Internal span
    countSpansByName(traces, 'GET /welcome') == 1 // akka Server span

    cleanup:
    stopTarget()

    where:
    jdk << [8, 11, 15]
  }

}
