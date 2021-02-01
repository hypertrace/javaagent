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

package org.hypertrace.agent.testing;

import io.opentelemetry.sdk.trace.data.SpanData;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public abstract class AbstractHttpClientTest extends AbstractInstrumenterTest {

  protected static final TestHttpServer testHttpServer = new TestHttpServer();

  public static final String ECHO_PATH = "http://localhost:%d/echo";

  @BeforeAll
  public static void startServer() throws Exception {
    testHttpServer.start();
  }

  @AfterAll
  public static void closeServer() throws Exception {
    testHttpServer.close();
  }

  /**
   * Make request using client and return response status code
   * @param uri URI to send request to
   * @param headers Request headers
   * @param body Request body if applicable on given method
   * @param contentType Content-type of request body
   * @return status code of response
   */
  public abstract int doPostRequest(String uri, Map<String, String> headers, String body, String contentType) throws IOException;

  @Test
  public void echoJson() throws TimeoutException, InterruptedException, IOException {
    String body = "{\"foo\": \"bar\"}";
    Map<String, String> headers = new HashMap<>();
    headers.put("headerName", "headerValue");
    String uri = String.format(ECHO_PATH, testHttpServer.port());

    int status = doPostRequest(uri, headers, body, "application/json");

    Assertions.assertEquals(200, status);

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);

    assertEchoJson(clientSpan, body);
  }

  @Test
  public void echoUrlEncoded() throws TimeoutException, InterruptedException, IOException {
    String body = "key1=value1&key2=value2";
    Map<String, String> headers = new HashMap<>();
    headers.put("headerName", "headerValue");
    String uri = String.format(ECHO_PATH, testHttpServer.port());

    int status = doPostRequest(uri, headers, body, "application/x-www-form-urlencoded");

    Assertions.assertEquals(200, status);

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);

    assertEchoUrlEncoded(clientSpan, body);
  }

  @Test
  public void echoPlainText() throws TimeoutException, InterruptedException, IOException {
    String body = "foobar";
    Map<String, String> headers = new HashMap<>();
    headers.put("headerName", "headerValue");
    String uri = String.format(ECHO_PATH, testHttpServer.port());

    int status = doPostRequest(uri, headers, body, "text/plain");

    Assertions.assertEquals(200, status);

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);

    assertEchoPlainText(clientSpan);
  }


  public void assertResponseHeaders(SpanData spanData) {
    Assertions.assertEquals(
        TestHttpServer.RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(
                HypertraceSemanticAttributes.httpResponseHeader(
                    TestHttpServer.RESPONSE_HEADER_NAME)));
  }

  public void assertEchoJson(SpanData spanData, String requestBody) {
    assertResponseHeaders(spanData);
    Assertions.assertEquals(requestBody, spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY),
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
  }

  public void assertEchoUrlEncoded(SpanData spanData, String requestBody) {
    assertEchoJson(spanData, requestBody);
  }

  public void assertEchoPlainText(SpanData spanData) {
    assertResponseHeaders(spanData);
    Assertions.assertNull(
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    Assertions.assertNull(
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
  }

}
