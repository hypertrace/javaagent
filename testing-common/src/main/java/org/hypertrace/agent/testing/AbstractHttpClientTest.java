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

import io.opentelemetry.javaagent.instrumentation.api.Pair;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public abstract class AbstractHttpClientTest extends AbstractInstrumenterTest {

  protected static final TestHttpServer testHttpServer = new TestHttpServer();

  private final boolean hasResponseBodySpan;

  public AbstractHttpClientTest(boolean hasResponseBodySpan) {
    this.hasResponseBodySpan = hasResponseBodySpan;
  }

  private static final String ECHO_PATH_FORMAT = "http://localhost:%d/echo";
  private static final String GET_NO_CONTENT_PATH_FORMAT = "http://localhost:%d/get_no_content";
  private static final String GET_JSON_PATH_FORMAT = "http://localhost:%d/get_json";

  private static final Map<String, String> headers;
  private static final String HEADER_NAME = "headerName";
  private static final String HEADER_VALUE = "headerValue";

  static {
    headers = new HashMap<>();
    headers.put("headerName", "headerValue");
  }

  @BeforeAll
  public static void startServer() throws Exception {
    testHttpServer.start();
  }

  @AfterAll
  public static void closeServer() throws Exception {
    testHttpServer.close();
  }

  /**
   * Make request using client and return response status code and body
   *
   * @param uri URI to send request to
   * @param headers Request headers
   * @param body Request body
   * @param contentType Content-type of request body
   * @return status code and body of response
   */
  public abstract Pair<Integer, String> doPostRequest(
      String uri, Map<String, String> headers, String body, String contentType) throws IOException;

  /**
   * Make request using client and return response status code and body
   *
   * @param uri URI to send request to
   * @param headers Request headers
   * @return status code and body of response
   */
  public abstract Pair<Integer, String> doGetRequest(String uri, Map<String, String> headers)
      throws IOException;

  @Test
  public void echoJson() throws TimeoutException, InterruptedException, IOException {
    String body = "{\"foo\": \"bar\"}";
    String uri = String.format(ECHO_PATH_FORMAT, testHttpServer.port());

    Pair<Integer, String> statusBodyPair = doPostRequest(uri, headers, body, "application/json");

    Assertions.assertEquals(200, statusBodyPair.getLeft());
    Assertions.assertEquals(body, statusBodyPair.getRight());

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());

    if (hasResponseBodySpan) {
      Assertions.assertEquals(2, traces.get(0).size());
      SpanData clientSpan = traces.get(0).get(0);
      SpanData responseBodySpan = traces.get(0).get(1);
      assertEchoJsonSpans(clientSpan, responseBodySpan, body);
    } else {
      Assertions.assertEquals(1, traces.get(0).size());
      SpanData clientSpan = traces.get(0).get(0);
      assertEchoJson(clientSpan, body);
    }
  }

  @Test
  public void echoUrlEncoded() throws TimeoutException, InterruptedException, IOException {
    String body = "key1=value1&key2=value2";
    String uri = String.format(ECHO_PATH_FORMAT, testHttpServer.port());

    Pair<Integer, String> statusBodyPair =
        doPostRequest(uri, headers, body, "application/x-www-form-urlencoded");

    Assertions.assertEquals(200, statusBodyPair.getLeft());
    Assertions.assertEquals(body, statusBodyPair.getRight());

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());

    if (hasResponseBodySpan) {
      Assertions.assertEquals(2, traces.get(0).size());
      SpanData clientSpan = traces.get(0).get(0);
      SpanData responseBodySpan = traces.get(0).get(1);
      assertEchoUrlEncodedSpans(clientSpan, responseBodySpan, body);
    } else {
      Assertions.assertEquals(1, traces.get(0).size());
      SpanData clientSpan = traces.get(0).get(0);
      assertEchoUrlEncoded(clientSpan, body);
    }
  }

  @Test
  public void echoPlainText() throws TimeoutException, InterruptedException, IOException {
    String body = "foobar";
    String uri = String.format(ECHO_PATH_FORMAT, testHttpServer.port());

    Pair<Integer, String> statusBodyPair = doPostRequest(uri, headers, body, "text/plain");

    Assertions.assertEquals(200, statusBodyPair.getLeft());
    Assertions.assertEquals(body, statusBodyPair.getRight());

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);

    assertNullBodies(clientSpan);
  }

  @Test
  public void getNoContent() throws IOException, TimeoutException, InterruptedException {
    String uri = String.format(GET_NO_CONTENT_PATH_FORMAT, testHttpServer.port());

    Pair<Integer, String> statusBodyPair = doGetRequest(uri, headers);

    Assertions.assertEquals(204, statusBodyPair.getLeft());
    Assertions.assertNull(statusBodyPair.getRight());

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);

    assertNullBodies(clientSpan);
  }

  @Test
  public void getJson() throws IOException, TimeoutException, InterruptedException {
    String uri = String.format(GET_JSON_PATH_FORMAT, testHttpServer.port());

    Pair<Integer, String> statusBodyPair = doGetRequest(uri, headers);

    Assertions.assertEquals(200, statusBodyPair.getLeft());
    Assertions.assertEquals(TestHttpServer.GetJsonHandler.RESPONSE_BODY, statusBodyPair.getRight());

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());

    if (hasResponseBodySpan) {
      Assertions.assertEquals(2, traces.get(0).size());
      SpanData clientSpan = traces.get(0).get(0);
      SpanData responseBodySpan = traces.get(0).get(1);
      Assertions.assertNull(
          clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
      Assertions.assertEquals(
          TestHttpServer.GetJsonHandler.RESPONSE_BODY,
          responseBodySpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    } else {
      Assertions.assertEquals(1, traces.get(0).size());
      SpanData clientSpan = traces.get(0).get(0);
      Assertions.assertNull(
          clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
      Assertions.assertEquals(
          TestHttpServer.GetJsonHandler.RESPONSE_BODY,
          clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    }
  }

  private void assertHeaders(SpanData spanData) {
    Assertions.assertEquals(
        TestHttpServer.RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(
                HypertraceSemanticAttributes.httpResponseHeader(
                    TestHttpServer.RESPONSE_HEADER_NAME)));
    Assertions.assertEquals(
        HEADER_VALUE,
        spanData.getAttributes().get(HypertraceSemanticAttributes.httpRequestHeader(HEADER_NAME)));
  }

  private void assertEchoJson(SpanData spanData, String requestBody) {
    Assertions.assertEquals(
        requestBody, spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY),
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
  }

  private void assertEchoJsonSpans(
      SpanData clientSpan, SpanData responseBodySpan, String requestBody) {
    Assertions.assertEquals(
        requestBody,
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY),
        responseBodySpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  private void assertEchoUrlEncoded(SpanData spanData, String requestBody) {
    assertEchoJson(spanData, requestBody);
  }

  private void assertEchoUrlEncodedSpans(
      SpanData clientSpan, SpanData responseBodySpan, String requestBody) {
    assertEchoJsonSpans(clientSpan, responseBodySpan, requestBody);
  }

  private void assertNullBodies(SpanData spanData) {
    assertHeaders(spanData);
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
  }
}
