/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hypertrace.agent.smoketest

import io.opentelemetry.proto.common.v1.KeyValue
import okhttp3.MediaType
import okhttp3.RequestBody
import spock.lang.Ignore
import spock.lang.IgnoreIf

import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Stream

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

    def port = target.getMappedPort(8080)
    String url = "http://localhost:${port}/app/greeting"
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
    traces.countFilteredAttributes("http.scheme", "http") == 2
    traces.countFilteredAttributes("http.target", "/app/greeting") == 1
    traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.host.port", port, "int").count() == 1 ||
            traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.sock.host.port", port, "int").count() == 1


    and: "Client and server spans for the remote call"
    // client span still has the http.url attribute
    traces.countFilteredAttributes("http.url", "http://localhost:8080/app/headers") == 1
    traces.countFilteredAttributes("http.target", "/app/headers") == 1
    traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.host.port", port, "int").count() == 1 ||
            traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.sock.host.port", port, "int").count() == 1


    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer request response capture test smoke test on JDK #jdk"(String appServer, String jdk) {
    assumeTrue(testSmoke())

    def port = target.getMappedPort(8080)
    String url = "http://localhost:${port}/app/echo"
    MediaType JSON = MediaType.parse("application/json; charset=utf-8")
    String requestData = "{\"greeting\" : \"Hello\",\"name\" : \"John\"}"
    RequestBody requestBody = RequestBody.create(requestData, JSON);
    def request = new Request.Builder().url(url).post(requestBody).build();

    when:
    def response = CLIENT.newCall(request).execute()
    TraceInspector traces = new TraceInspector(waitForTraces())
    Set<String> traceIds = traces.traceIds
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
    traces.countFilteredAttributes("http.scheme", "http") == 2
    traces.countFilteredAttributes("http.target", "/app/echo") == 1
    traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.host.port", port, "int").count() == 1 ||
            traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.sock.host.port", port, "int").count() == 1


    and: "Client and server spans for the remote call"
    // client span still has the http.url attribute
    traces.countFilteredAttributes("http.url", "http://localhost:8080/app/headers") == 1
    traces.countFilteredAttributes("http.target", "/app/headers") == 1
    traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.host.port", port, "int").count() == 1 ||
            traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.sock.host.port", port, "int").count() == 1


    and: "response body attribute should be present"
    traces.countFilteredAttributes("http.response.body") == 1

    and: "request body attribute should be present"
    traces.countFilteredAttributes("http.request.body") == 1

    and: "Request body should be same as sent content"
    traces.getFilteredAttributeValue("http.request.body") == requestData

    and: "Response body should be same as sent content"
    traces.getFilteredAttributeValue("http.response.body") == requestData

    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer test static file found on JDK #jdk"(String appServer, String jdk) {
    def port = target.getMappedPort(8080)
    String url = "http://localhost:${port}/app/hello.txt"
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
    traces.countFilteredAttributes("http.scheme", "http") == 1
    traces.countFilteredAttributes("http.target", "/app/hello.txt") == 1
    traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.host.port", port, "int").count() == 1 ||
            traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.sock.host.port", port, "int").count() == 1


    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer test static file not found on JDK #jdk"(String appServer, String jdk) {
    def port = target.getMappedPort(8080)
    String url = "http://localhost:${port}/app/file-that-does-not-exist"
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
    traces.countFilteredAttributes("http.scheme", "http") == 1
    traces.countFilteredAttributes("http.target", "/app/file-that-does-not-exist") == 1
    traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.host.port", port, "int").count() == 1 ||
            traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.sock.host.port", port, "int").count() == 1


    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  @IgnoreIf({ System.getProperty("os.name").contains("windows") })
  def "#appServer test request for WEB-INF/web.xml on JDK #jdk"(String appServer, String jdk) {
    // TODO https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/2499
    if (getTargetImage(appServer, jdk).toLowerCase().contains("wildfly")) {
      return
    }

    assumeTrue(testRequestWebInfWebXml())

    def port = target.getMappedPort(8080)
    String url = "http://localhost:${ port}/app/WEB-INF/web.xml"
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
    traces.countFilteredAttributes("http.scheme", "http") == 1
    traces.countFilteredAttributes("http.target", "/app/WEB-INF/web.xml") == 1
    traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.host.port", port, "int").count() == 1 ||
            traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.sock.host.port", port, "int").count() == 1


    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer test request with error JDK #jdk"(String appServer, String jdk) {
    assumeTrue(testException())

    def port = target.getMappedPort(8080)
    String url = "http://localhost:${port}/app/exception"
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
    traces.countFilteredAttributes("http.scheme", "http") == 1
    traces.countFilteredAttributes("http.target", "/app/exception") == 1
    traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.host.port", port, "int").count() == 1 ||
            traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.sock.host.port", port, "int").count() == 1


    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer test request outside deployed application JDK #jdk"(String appServer, String jdk) {
    // TODO https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/2499
    if (getTargetImage(appServer, jdk).toLowerCase().contains("wildfly")) {
      return
    }


    def port = target.getMappedPort(8080)
    String url = "http://localhost:${port}/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless"
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
    traces.countFilteredAttributes("http.scheme", "http") == 1
    traces.countFilteredAttributes("http.target", "/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless") == 1
    traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.host.port", 8080, "int").count() == 1 ||
            traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.sock.host.port", 8080, "int").count() == 1

    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  @Unroll
  def "#appServer async smoke test on JDK #jdk"(String appServer, String jdk) {
    assumeTrue(testAsyncSmoke())

    def port = target.getMappedPort(8080)
    String url = "http://localhost:${ port}/app/asyncgreeting"
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
    traces.countFilteredAttributes("http.scheme", "http") == 2
    traces.countFilteredAttributes("http.target", "/app/asyncgreeting") == 1
    traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.host.port", port, "int").count() == 1 ||
            traces.filterSpansByAttributes(traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.sock.host.port", port, "int").count() == 1


    and: "Client and server spans for the remote call"
    // client span still has the http.url attribute
    traces.countFilteredAttributes("http.url", "http://localhost:8080/app/headers") == 1
    traces.countFilteredAttributes("http.target", "/app/headers") == 1
    traces.filterSpansByAttributes(
            traces.filterSpansByAttributes(traces.getSpanStream(), "net.host.name", "localhost", "string"), "net.host.port", port, "int").count() == 1

    cleanup:
    response?.close()

    where:
    [appServer, jdk] << getTestParams()
  }

  protected String getSpanName(String path) {
    switch (path) {
      case "/app/greeting":
      case "/app/headers":
      case "/app/exception":
      case "/app/asyncgreeting":
        return "GET " + path
      case "/app/echo":
        return "POST " + path
      case "/app/hello.txt":
      case "/app/file-that-does-not-exist":
        return "GET " + "/app/*"
    }
    return "GET"
  }

  protected List<List<Object>> getTestParams() {
    return [
            [serverVersion, jdk]
    ]
  }
}
