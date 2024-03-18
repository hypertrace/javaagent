/*
 * Copyright The Hypertrace Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.javaagent.instrumentation.hypertrace.undertow.v1_4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.opentelemetry.proto.trace.v1.Span;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.MediaType;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class UndertowInstrumentationTest extends AbstractInstrumenterTest {

  private static Undertow server;
  private static int availablePort;

  @BeforeAll
  static void startServer() throws ServletException {
    try (ServerSocket socket = new ServerSocket(0)) {
      availablePort = socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    final String contextPath = "/myapp";
    final DeploymentInfo deploymentInfo =
        Servlets.deployment()
            .setClassLoader(TestServlet.class.getClassLoader())
            .setContextPath(contextPath)
            .setDeploymentName("myapp.war")
            .addServlet(Servlets.servlet("TestServlet", TestServlet.class).addMapping("/*"));
    final DeploymentManager deploymentManager =
        Servlets.defaultContainer().addDeployment(deploymentInfo);
    deploymentManager.deploy();
    final HttpHandler servletHandler = deploymentManager.start();
    final PathHandler rootHandler =
        Handlers.path(Handlers.redirect(contextPath)).addPrefixPath(contextPath, servletHandler);

    server =
        Undertow.builder()
            .addHttpListener(availablePort, "localhost")
            .setHandler(rootHandler)
            .build();
    server.start();
  }

  @AfterAll
  static void stopServer() {
    server.stop();
  }

  @Test
  void postUrlEncoded() throws InterruptedException, TimeoutException, IOException {
    final String requestBody = "echo=bar";

    try (Response response =
        this.httpClient
            .newCall(
                new Builder()
                    .url("http://localhost:" + availablePort + "/myapp/")
                    .post(
                        RequestBody.create(
                            requestBody, MediaType.get("application/x-www-form-urlencoded")))
                    .build())
            .execute()) {
      assertEquals(200, response.code());
    }

    TEST_WRITER.waitForTraces(1);
    List<List<Span>> traces =
        TEST_WRITER.waitForSpans(
            1,
            span ->
                span.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT)
                    || span.getKind().equals(Span.SpanKind.SPAN_KIND_INTERNAL));
    Assertions.assertEquals(1, traces.size());
    List<Span> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    Span span = trace.get(0);
    assertEquals(
        "{\"echo\": \"bar\"}",
        TEST_WRITER.getAttributesMap(span).get("http.response.body").getStringValue());
    assertEquals(
        requestBody, TEST_WRITER.getAttributesMap(span).get("http.request.body").getStringValue());

    // make a second request to an endpoint that should leverage servlet instrumentation
    try (Response response =
        this.httpClient
            .newCall(
                new Builder()
                    .url("http://localhost:" + availablePort + "/myapp/")
                    .put(
                        RequestBody.create(
                            requestBody, MediaType.get("application/x-www-form-urlencoded")))
                    .build())
            .execute()) {
      assertEquals(200, response.code());
    }

    TEST_WRITER.waitForTraces(2);
    List<List<Span>> putTraces =
        TEST_WRITER.waitForSpans(
            2,
            span1 ->
                span1.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT)
                    || span1.getKind().equals(Span.SpanKind.SPAN_KIND_INTERNAL));
    Assertions.assertEquals(2, putTraces.size());
    List<Span> putTrace = putTraces.get(1);
    Assertions.assertEquals(1, putTrace.size());
    Span putSpan = putTrace.get(0);
    assertEquals(
        "echo=bar&append=true",
        TEST_WRITER.getAttributesMap(putSpan).get("http.response.body").getStringValue());
    assertEquals(
        requestBody,
        TEST_WRITER.getAttributesMap(putSpan).get("http.request.body").getStringValue());

    // make a third request to an endpoint that should return JSON
    try (Response response =
        this.httpClient
            .newCall(
                new Builder()
                    .url("http://localhost:" + availablePort + "/myapp/?format=json")
                    .get()
                    .build())
            .execute()) {
      assertEquals(200, response.code());
    }

    TEST_WRITER.waitForTraces(3);
    final List<List<Span>> getJsonTraces =
        TEST_WRITER.waitForSpans(
            3,
            span1 ->
                span1.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT)
                    || span1.getKind().equals(Span.SpanKind.SPAN_KIND_INTERNAL));
    Assertions.assertEquals(3, getJsonTraces.size());
    final List<Span> getJsonTrace = getJsonTraces.get(2);
    Assertions.assertEquals(1, getJsonTrace.size());
    final Span getJsonSpan = getJsonTrace.get(0);
    assertEquals(
        "{\"message\": \"Hello World\"}",
        TEST_WRITER.getAttributesMap(getJsonSpan).get("http.response.body").getStringValue());
    // empty request body should not be captured
    assertNull(TEST_WRITER.getAttributesMap(getJsonSpan).get("http.request.body"));

    // make a four request to an endpoint that should return HTML
    try (Response response =
        this.httpClient
            .newCall(
                new Builder().url("http://localhost:" + availablePort + "/myapp/").get().build())
            .execute()) {
      assertEquals(200, response.code());
    }

    TEST_WRITER.waitForTraces(4);
    final List<List<Span>> getHtmlTraces =
        TEST_WRITER.waitForSpans(
            4,
            span1 ->
                span1.getKind().equals(Span.SpanKind.SPAN_KIND_CLIENT)
                    || span1.getKind().equals(Span.SpanKind.SPAN_KIND_INTERNAL));
    Assertions.assertEquals(4, getHtmlTraces.size());
    final List<Span> getHtmlTrace = getHtmlTraces.get(3);
    Assertions.assertEquals(1, getHtmlTrace.size());
    final Span getHtmlSpan = getHtmlTrace.get(0);
    // HTML body should not be captured
    assertNull(TEST_WRITER.getAttributesMap(getHtmlSpan).get("http.response.body"));
    // request body should not be captured
    assertNull(TEST_WRITER.getAttributesMap(getHtmlSpan).get("http.request.body"));
  }

  public static final class TestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      final String format = req.getParameter("format");
      final byte[] responseBody;
      if ("json".equals(format)) {
        responseBody = "{\"message\": \"Hello World\"}".getBytes(StandardCharsets.UTF_8);
        resp.setContentType("application/json; charset=UTF-8");

      } else {
        responseBody = "<h1>Hello World</h1>".getBytes(StandardCharsets.UTF_8);
        resp.setContentType("text/html; charset=UTF-8");
      }
      resp.setStatus(200);
      resp.setContentLength(responseBody.length);
      try (ServletOutputStream servletOutputStream = resp.getOutputStream()) {
        servletOutputStream.write(responseBody);
      }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException {
      final Map<String, String[]> parameterMap = req.getParameterMap();
      final String echo = parameterMap.get("echo")[0];
      final byte[] responseBody = ("{\"echo\": \"" + echo + "\"}").getBytes(StandardCharsets.UTF_8);
      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setContentLength(responseBody.length);
      try (ServletOutputStream servletOutputStream = resp.getOutputStream()) {
        servletOutputStream.write(responseBody);
      }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

      final String body;
      try (ServletInputStream inputStream = req.getInputStream()) {
        final int contentLength = req.getContentLength();
        final byte[] requestBytes = new byte[contentLength];
        inputStream.read(requestBytes);
        body = new String(requestBytes) + "&append=true";
      }
      resp.setStatus(200);
      resp.setContentType("application/x-www-form-urlencoded");
      resp.setContentLength(body.length());
      try (ServletOutputStream outputStream = resp.getOutputStream()) {
        outputStream.write(body.getBytes(StandardCharsets.UTF_8));
      }
    }
  }
}
