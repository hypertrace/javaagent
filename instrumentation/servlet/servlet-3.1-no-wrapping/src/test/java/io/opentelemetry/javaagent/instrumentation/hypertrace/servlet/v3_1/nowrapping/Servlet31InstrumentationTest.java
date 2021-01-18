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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Servlet31InstrumentationTest extends AbstractInstrumenterTest {
  private static final String REQUEST_BODY = "hello";
  private static final String REQUEST_HEADER = "requestheader";
  private static final String REQUEST_HEADER_VALUE = "requestvalue";

  private static Server server = new Server(0);
  private static int serverPort;

  @BeforeAll
  public static void startServer() throws Exception {
    ServletContextHandler handler = new ServletContextHandler();
    handler.addServlet(TestServlet.class, "/test");
    server.setHandler(handler);
    server.start();
    serverPort = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
  }

  @AfterAll
  public static void stopServer() throws Exception {
    server.stop();
  }

  @Test
  public void postJson() throws Exception {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/test", serverPort))
            .post(RequestBody.create(REQUEST_BODY, MediaType.get("application/json")))
            .header(REQUEST_HEADER, REQUEST_HEADER_VALUE)
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
      Assertions.assertEquals(TestServlet.RESPONSE_BODY, response.body().string());
    }

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    List<SpanData> spans = traces.get(0);
    Assertions.assertEquals(1, spans.size());
    SpanData spanData = spans.get(0);
    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER)));
    Assertions.assertEquals(
        TestServlet.RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(TestServlet.RESPONSE_HEADER)));
    Assertions.assertEquals(
        REQUEST_BODY, spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        TestServlet.RESPONSE_BODY,
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }
}
