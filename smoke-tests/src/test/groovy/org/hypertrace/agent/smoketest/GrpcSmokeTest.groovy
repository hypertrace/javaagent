/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hypertrace.agent.smoketest

import io.grpc.ManagedChannelBuilder
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc
import spock.lang.Unroll

class GrpcSmokeTest extends SmokeTest {

  protected String getTargetImage(String jdk, String serverVersion) {
    "ghcr.io/open-telemetry/java-test-containers:smoke-grpc-jdk$jdk-20210129.520311770"
  }

  @Unroll
  def "grpc smoke test on JDK #jdk"(int jdk) {
    setup:
    startTarget(jdk)

    def channel = ManagedChannelBuilder.forAddress("localhost", target.getMappedPort(8080))
            .usePlaintext()
            .build()
    def stub = TraceServiceGrpc.newBlockingStub(channel)

    when:
    stub.export(ExportTraceServiceRequest.getDefaultInstance())
    Collection<ExportTraceServiceRequest> traces = waitForTraces()

    then:
    countSpansByName(traces, 'opentelemetry.proto.collector.trace.v1.TraceService/Export') == 1
    countSpansByName(traces, 'TestService.withSpan') == 1

    cleanup:
    stopTarget()
    channel.shutdown()

    where:
    jdk << [8, 11, 15]
  }
}
