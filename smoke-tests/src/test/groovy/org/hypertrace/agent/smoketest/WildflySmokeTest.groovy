/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hypertrace.agent.smoketest

import io.opentelemetry.proto.trace.v1.Span
import okhttp3.Request
import spock.lang.Unroll

@AppServer(version = "13.0.0.Final", jdk = "8")
@AppServer(version = "13.0.0.Final", jdk = "8-openj9")
@AppServer(version = "17.0.1.Final", jdk = "11")
@AppServer(version = "17.0.1.Final", jdk = "11-openj9")
@AppServer(version = "21.0.0.Final", jdk = "11")
@AppServer(version = "21.0.0.Final", jdk = "11-openj9")
class WildflySmokeTest extends AppServerTest {

  protected String getTargetImage(String jdk, String serverVersion) {
    "hypertrace/java-agent-test-containers:wildfly-${serverVersion}-jdk$jdk-20210226.602156580"
  }

  // TODO These re ignored in the superclass for Wildfly
//  @Ignore
//  @Unroll
//  def "#appServer test request outside deployed application JDK #jdk"(String appServer, String jdk) {
//  }
//
//  @Ignore
//  def "#appServer test request for WEB-INF/web.xml on JDK #jdk"(String appServer, String jdk) {
//  }

  @Unroll
  def "JSP smoke test on WildFly"() {
    String url = "http://localhost:${target.getMappedPort(8080)}/app/jsp"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    TraceInspector traces = new TraceInspector(waitForTraces())
    String responseBody = response.body().string()

    then:
    response.successful
    responseBody.contains("Successful JSP test")

    traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER) == 1

    traces.countSpansByName('GET /app/jsp') == 1

    where:
    [appServer, jdk] << getTestParams()
  }
}
