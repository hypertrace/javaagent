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

package io.opentelemetry.instrumentation.hypertrace.servlet.v3_0;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Servlet30Test extends AbstractInstrumenterTest {

  @Test
  public void simpleServlet() throws Exception {
    Server server = new Server(0);
    ServletContextHandler handler = new ServletContextHandler();
    handler.addServlet(TestServlet.class, "/test");

    server.setHandler(handler);
    server.start();

    int serverPort = server.getConnectors()[0].getLocalPort();

    String requestBody = "hello";
    String requestHeader = "requestheader";
    String requestHeaderValue = "requestvalue";
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/test", serverPort))
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .header(requestHeader, requestHeaderValue)
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
        requestBody, spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        requestHeaderValue,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(requestHeader)));

    Assertions.assertEquals(
        TestServlet.RESPONSE_BODY,
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    Assertions.assertEquals(
        TestServlet.RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(TestServlet.RESPONSE_HEADER)));
    server.stop();
  }
}
