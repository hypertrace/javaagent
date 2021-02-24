/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hypertrace.agent.smoketest

import okhttp3.MediaType
import okhttp3.RequestBody

import static org.junit.Assume.assumeTrue

import io.opentelemetry.proto.trace.v1.Span
import okhttp3.Request
import org.junit.runner.RunWith
import spock.lang.Shared
import spock.lang.Unroll

@RunWith(AppServerTestRunner)
abstract class AppServerTest extends SmokeTest {
  @Shared
  String jdk
  @Shared
  String serverVersion

  def setupSpec() {
    def appServer = AppServerTestRunner.currentAppServer(this.getClass())
    serverVersion = appServer.version()
    jdk = appServer.jdk()
    startTarget(jdk, serverVersion)
  }

  def cleanupSpec() {
    stopTarget()
  }

  boolean testSmoke() {
    true
  }

  boolean testAsyncSmoke() {
    true
  }

  boolean testException() {
    true
  }

  boolean testRequestWebInfWebXml() {
    true
  }

  //TODO add assert that server spans were created by servers, not by servlets
  @Unroll
  def "#appServer smoke test on JDK #jdk"(String appServer, String jdk) {
    assumeTrue(testSmoke())

    String url = "http://localhost:${target.getMappedPort(8080)}/app/greeting"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    TraceInspector traces = new TraceInspector(waitForTraces())
    Set<String> traceIds = traces.traceIds
    String responseBody = response.body().string()

    then: "There is one trace"
    traceIds.size() == 1

    and: "trace id is present in the HTTP headers as reported by the called endpoint"
    responseBody.contains(traceIds.find())

    and: "Server spans in the distributed trace"
    traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER) == 2

    and: "Expected span names"
    traces.countSpansByName(getSpanName('/app/greeting')) == 1
    traces.countSpansByName(getSpanName('/app/headers')) == 1

    and: "The span for the initial web request"
    traces.countFilteredAttributes("http.url", url) == 1

    and: "Client and server spans for the remote call"
    traces.countFilteredAttributes("http.url", "http://localhost:8080/app/headers") == 2

    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer request response capture test smoke test on JDK #jdk"(String appServer, String jdk) {
    assumeTrue(testSmoke())

    String url = "http://localhost:${target.getMappedPort(8080)}/app/echo"
    MediaType JSON = MediaType.parse("application/json; charset=utf-8")
    String stringBody = "{\"greeting\" : \"Hello\",\"name\" : \"John\"}"
    RequestBody requestBody = RequestBody.create(stringBody, JSON);
    def request = new Request.Builder().url(url).post(requestBody).build();

    when:
    def response = CLIENT.newCall(request).execute()
    TraceInspector traces = new TraceInspector(waitForTraces())
    Set<String> traceIds = traces.traceIds
    String responseBody = response.body().string()
    String headerValue = new String(Base64.getDecoder().decode(response.header("Header-Dump")));

    then: "There is one trace"
    traceIds.size() == 1

    and: "trace id is present in the HTTP headers as reported by the called endpoint"
    headerValue.contains(traceIds.find())

    and: "Server spans in the distributed trace"
    traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER) == 2

    and: "Expected span names"
    traces.countSpansByName(getSpanName('/app/echo')) == 1
    traces.countSpansByName(getSpanName('/app/headers')) == 1

    and: "The span for the initial web request"
    traces.countFilteredAttributes("http.url", url) == 1

    and: "Client and server spans for the remote call"
    traces.countFilteredAttributes("http.url", "http://localhost:8080/app/headers") == 2

    and: "response body attribute should be present"
    traces.countFilteredAttributes("http.response.body") == 1

    and: "request body attribute should be present"
    traces.countFilteredAttributes("http.request.body") == 1

    and: "Request body should be same as sent content"
    traces.getFilteredAttributeValue("http.request.body") == stringBody

    and: "Response body should be same as sent content"
    traces.getFilteredAttributeValue("http.response.body") == stringBody

    and: "Response body should be same as received response body"
    traces.getFilteredAttributeValue("http.response.body") == responseBody

    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer test static file found on JDK #jdk"(String appServer, String jdk) {
    String url = "http://localhost:${target.getMappedPort(8080)}/app/hello.txt"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    TraceInspector traces = new TraceInspector(waitForTraces())
    Set<String> traceIds = traces.traceIds
    String responseBody = response.body().string()

    then: "There is one trace"
    traceIds.size() == 1

    and: "Response contains Hello"
    responseBody.contains("Hello")

    and: "There is one server span"
    traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER) == 1

    and: "Expected span names"
    traces.countSpansByName(getSpanName('/app/hello.txt')) == 1

    and: "The span for the initial web request"
    traces.countFilteredAttributes("http.url", url) == 1

    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer test static file not found on JDK #jdk"(String appServer, String jdk) {
    String url = "http://localhost:${target.getMappedPort(8080)}/app/file-that-does-not-exist"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    TraceInspector traces = new TraceInspector(waitForTraces())
    Set<String> traceIds = traces.traceIds

    then: "There is one trace"
    traceIds.size() == 1

    and: "Response code is 404"
    response.code() == 404

    and: "There is one server span"
    traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER) == 1

    and: "Expected span names"
    traces.countSpansByName(getSpanName('/app/file-that-does-not-exist')) == 1

    and: "The span for the initial web request"
    traces.countFilteredAttributes("http.url", url) == 1

    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer test request for WEB-INF/web.xml on JDK #jdk"(String appServer, String jdk) {
    assumeTrue(testRequestWebInfWebXml())

    String url = "http://localhost:${target.getMappedPort(8080)}/app/WEB-INF/web.xml"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    TraceInspector traces = new TraceInspector(waitForTraces())
    Set<String> traceIds = traces.traceIds

    then: "There is one trace"
    traceIds.size() == 1

    and: "Response code is 404"
    response.code() == 404

    and: "There is one server span"
    traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER) == 1

    and: "Expected span names"
    traces.countSpansByName(getSpanName('/app/WEB-INF/web.xml')) == 1

    and: "The span for the initial web request"
    traces.countFilteredAttributes("http.url", url) == 1

    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer test request with error JDK #jdk"(String appServer, String jdk) {
    assumeTrue(testException())

    String url = "http://localhost:${target.getMappedPort(8080)}/app/exception"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    TraceInspector traces = new TraceInspector(waitForTraces())
    Set<String> traceIds = traces.traceIds

    then: "There is one trace"
    traceIds.size() == 1

    and: "Response code is 500"
    response.code() == 500

    and: "There is one server span"
    traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER) == 1

    and: "Expected span names"
    traces.countSpansByName(getSpanName('/app/exception')) == 1

    and: "There is one exception"
    traces.countFilteredEventAttributes('exception.message', 'This is expected') == 1

    and: "The span for the initial web request"
    traces.countFilteredAttributes("http.url", url) == 1

    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer test request outside deployed application JDK #jdk"(String appServer, String jdk) {
    String url = "http://localhost:${target.getMappedPort(8080)}/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    TraceInspector traces = new TraceInspector(waitForTraces())
    Set<String> traceIds = traces.traceIds

    then: "There is one trace"
    traceIds.size() == 1

    and: "Response code is 404"
    response.code() == 404

    and: "There is one server span"
    traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER) == 1

    and: "Expected span names"
    traces.countSpansByName(getSpanName('/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless')) == 1

    and: "The span for the initial web request"
    traces.countFilteredAttributes("http.url", url) == 1

    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer async smoke test on JDK #jdk"(String appServer, String jdk) {
    assumeTrue(testAsyncSmoke())

    String url = "http://localhost:${target.getMappedPort(8080)}/app/asyncgreeting"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = CLIENT.newCall(request).execute()
    TraceInspector traces = new TraceInspector(waitForTraces())
    Set<String> traceIds = traces.traceIds
    String responseBody = response.body().string()

    then: "There is one trace"
    traceIds.size() == 1

    and: "trace id is present in the HTTP headers as reported by the called endpoint"
    responseBody.contains(traceIds.find())

    and: "Server spans in the distributed trace"
    traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER) == 2

    and: "Expected span names"
    traces.countSpansByName(getSpanName('/app/asyncgreeting')) == 1
    traces.countSpansByName(getSpanName('/app/headers')) == 1

    and: "The span for the initial web request"
    traces.countFilteredAttributes("http.url", url) == 1

    and: "Client and server spans for the remote call"
    traces.countFilteredAttributes("http.url", "http://localhost:8080/app/headers") == 2

    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  protected abstract String getSpanName(String path);

  protected List<List<Object>> getTestParams() {
    return [
            [serverVersion, jdk]
    ]
  }
}