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

package io.opentelemetry.javaagent.instrumentation.hypertrace.struts;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.servlet.DispatcherType;

import io.opentelemetry.sdk.trace.data.SpanData;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.struts2.dispatcher.ng.filter.StrutsPrepareAndExecuteFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.FileResource;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StrutsInstrumentationTest extends AbstractInstrumenterTest {

  private static final String REQUEST_BODY = "hello";
  private static final String REQUEST_HEADER = "requestheader";
  private static final String REQUEST_HEADER_VALUE = "requestvalue";
  private static final String RESPONSE_HEADER = "headerName";
  private static final String RESPONSE_HEADER_VALUE = "headerValue";

  private static Server server = new Server(0);
  private static int serverPort;

  @BeforeAll
  public static void startServer() throws Exception {
    ServletContextHandler handler = new ServletContextHandler();
    FileResource resource = new FileResource(StrutsInstrumentationTest.class.getResource("/"));
    handler.setContextPath("/context");
    handler.setBaseResource(resource);
    handler.addServlet(DefaultServlet.class, "/");
    handler.addFilter(StrutsPrepareAndExecuteFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
    server.setHandler(handler);
    server.start();
    serverPort = server.getConnectors()[0].getLocalPort();
  }

  @AfterAll
  public static void stopServer() throws Exception {
    server.stop();
  }

  @Test
  public void testBody() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/context/body", serverPort))
            .post(RequestBody.create(REQUEST_BODY, MediaType.get("application/x-www-form-urlencoded")))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      assertEquals(200, response.code());
    }

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    assertEquals(1, traces.size());
    List<SpanData> spans = traces.get(0);
    assertEquals(1, spans.size());
    SpanData spanData = spans.get(0);
    assertEquals("\"" + Struts2Action.sample + "\"", spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    assertEquals(REQUEST_BODY, spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
  }

  @Test
  public void testHeaders() throws IOException, TimeoutException, InterruptedException {
    Request request =
            new Request.Builder()
                    .url(String.format("http://localhost:%d/context/headers", serverPort))
                    .get()
                    .header(REQUEST_HEADER, REQUEST_HEADER_VALUE)
                    .build();
    try (Response response = httpClient.newCall(request).execute()) {
      assertEquals(200, response.code());
    }

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    assertEquals(1, traces.size());
    List<SpanData> spans = traces.get(0);
    assertEquals(1, spans.size());
    SpanData spanData = spans.get(0);
    assertEquals(RESPONSE_HEADER_VALUE, spanData.getAttributes().get(HypertraceSemanticAttributes.httpResponseHeader(RESPONSE_HEADER)));
    assertEquals(REQUEST_HEADER_VALUE, spanData.getAttributes().get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER)));
  }

  @Test
  public void testBlocking() throws IOException, TimeoutException, InterruptedException {
    Request request =
            new Request.Builder()
                    .url(String.format("http://localhost:%d/context/body", serverPort))
                    .get()
                    .header("mockblock", "true")
                    .build();
    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(403, response.code());
    }

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    List<SpanData> spans = traces.get(0);
    Assertions.assertEquals(1, spans.size());
    SpanData spanData = spans.get(0);
    Assertions.assertNull(
            spanData.getAttributes().get(HypertraceSemanticAttributes.httpResponseHeader(RESPONSE_HEADER)));
    Assertions.assertNull(
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

}
