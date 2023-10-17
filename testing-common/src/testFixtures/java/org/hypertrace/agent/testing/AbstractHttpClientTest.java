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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public abstract class AbstractHttpClientTest extends AbstractInstrumenterTest {

  private static final String ECHO_PATH_FORMAT = "http://localhost:%d/echo";
  private static final String GET_NO_CONTENT_PATH_FORMAT = "http://localhost:%d/get_no_content";
  private static final String GET_JSON_PATH_FORMAT = "http://localhost:%d/get_json";

  private static final String HEADER_NAME = "headerName";
  private static final String HEADER_VALUE = "headerValue";
  private static final Map<String, String> headers;

  protected static final TestHttpServer testHttpServer = new TestHttpServer();

  static {
    Map<String, String> headersMap = new HashMap<>();
    headersMap.put(HEADER_NAME, HEADER_VALUE);
    headers = Collections.unmodifiableMap(headersMap);
  }

  private final boolean hasResponseBodySpan;

  private FakeTransformer fakeTransformer;
  private String transformedClassDirName;

  public AbstractHttpClientTest(boolean hasResponseBodySpan) {
    this.hasResponseBodySpan = hasResponseBodySpan;
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
  public abstract Response doPostRequest(
      String uri, Map<String, String> headers, String body, String contentType)
      throws IOException, ExecutionException, InterruptedException, TimeoutException;

  /**
   * Make request using client and return response status code and body
   *
   * @param uri URI to send request to
   * @param headers Request headers
   * @return status code and body of response
   */
  public abstract Response doGetRequest(String uri, Map<String, String> headers)
      throws IOException, ExecutionException, InterruptedException, TimeoutException;

  @Test
  public void postJson_echo()
      throws TimeoutException, InterruptedException, IOException, ExecutionException {
    String body = "{\"foo\": \"bar\"}";
    String uri = String.format(ECHO_PATH_FORMAT, testHttpServer.port());

    Response response = doPostRequest(uri, headers, body, "application/json");

    Assertions.assertEquals(200, response.statusCode);
    Assertions.assertEquals(body, response.body);

    TEST_WRITER.waitForTraces(1);
    if (hasResponseBodySpan) {
      TEST_WRITER.waitForSpans(2);
    }
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    SpanData clientSpan = traces.get(0).get(0);

    if (hasResponseBodySpan) {
      Assertions.assertEquals(2, traces.get(0).size());
      SpanData responseBodySpan = traces.get(0).get(1);
      assertBodies(clientSpan, responseBodySpan, body, body);
      Assertions.assertNotNull(
          responseBodySpan
              .getAttributes()
              .get(AttributeKey.stringKey("http.response.header.content-type")));
    } else {
      Assertions.assertEquals(1, traces.get(0).size());
      assertRequestAndResponseBody(clientSpan, body, body);
    }
  }

  /**
   * For debugging unit tests, this function can be called to pass the class back through the OTEL
   * Transformer, and write the resulting class bytes to an external file.
   *
   * @param className
   */
  private void writeTransformedClass(String className) {
    if (fakeTransformer == null) {
      transformedClassDirName = System.getenv("TRANSFORMED_CLASS_DIR");
      if (transformedClassDirName != null && transformedClassDirName.length() > 0) {
        fakeTransformer = new FakeTransformer();
      }
    }

    if (fakeTransformer != null) {
      fakeTransformer.writeTransformedClass(className, transformedClassDirName);
    }
  }

  @Test
  public void postUrlEncoded_echo()
      throws TimeoutException, InterruptedException, IOException, ExecutionException {
    String body = "key1=value1&key2=value2";
    String uri = String.format(ECHO_PATH_FORMAT, testHttpServer.port());

    Response response = doPostRequest(uri, headers, body, "application/x-www-form-urlencoded");

    Assertions.assertEquals(200, response.statusCode);
    Assertions.assertEquals(body, response.body);

    TEST_WRITER.waitForTraces(1);
    if (hasResponseBodySpan) {
      TEST_WRITER.waitForSpans(2);
    }
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    SpanData clientSpan = traces.get(0).get(0);

    if (hasResponseBodySpan) {
      Assertions.assertEquals(2, traces.get(0).size());
      SpanData responseBodySpan = traces.get(0).get(1);
      assertBodies(clientSpan, responseBodySpan, body, body);
    } else {
      Assertions.assertEquals(1, traces.get(0).size());
      assertRequestAndResponseBody(clientSpan, body, body);
    }
  }

  @Test
  public void postPlainText_echo()
      throws TimeoutException, InterruptedException, IOException, ExecutionException {
    String body = "foobar";
    String uri = String.format(ECHO_PATH_FORMAT, testHttpServer.port());

    Response response = doPostRequest(uri, headers, body, "text/plain");

    Assertions.assertEquals(200, response.statusCode);
    Assertions.assertEquals(body, response.body);

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);

    assertHeaders(clientSpan);
    assertNoBodies(clientSpan);
  }

  @Test
  public void getNoContent()
      throws IOException, TimeoutException, InterruptedException, ExecutionException {
    String uri = String.format(GET_NO_CONTENT_PATH_FORMAT, testHttpServer.port());

    Response response = doGetRequest(uri, headers);

    Assertions.assertEquals(204, response.statusCode);
    Assertions.assertNull(response.body);

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);

    assertHeaders(clientSpan);
    assertNoBodies(clientSpan);
  }

  @Test
  public void getJson()
      throws IOException, TimeoutException, InterruptedException, ExecutionException {
    String uri = String.format(GET_JSON_PATH_FORMAT, testHttpServer.port());

    Response response = doGetRequest(uri, headers);

    Assertions.assertEquals(200, response.statusCode);
    Assertions.assertEquals(TestHttpServer.GetJsonHandler.RESPONSE_BODY, response.body);

    TEST_WRITER.waitForTraces(1);
    if (hasResponseBodySpan) {
      TEST_WRITER.waitForSpans(2);
    }
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    SpanData clientSpan = traces.get(0).get(0);
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));

    if (hasResponseBodySpan) {
      Assertions.assertEquals(2, traces.get(0).size());
      SpanData responseBodySpan = traces.get(0).get(1);
      Assertions.assertEquals(
          TestHttpServer.GetJsonHandler.RESPONSE_BODY,
          responseBodySpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    } else {
      Assertions.assertEquals(1, traces.get(0).size());
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

  private void assertRequestAndResponseBody(
      SpanData spanData, String requestBody, String responseBody) {
    Assertions.assertEquals(
        requestBody, spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        responseBody,
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  private void assertBodies(
      SpanData clientSpan, SpanData responseBodySpan, String requestBody, String responseBody) {
    Assertions.assertEquals(
        requestBody,
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        responseBody,
        responseBodySpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  private void assertNoBodies(SpanData spanData) {
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
  }

  public static String readInputStream(InputStream inputStream) throws IOException {
    StringBuilder textBuilder = new StringBuilder();

    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
      int c;
      while ((c = reader.read()) != -1) {
        textBuilder.append((char) c);
      }
    }
    return textBuilder.toString();
  }

  public static class Response {
    String body;
    int statusCode;

    public Response(String body, int statusCode) {
      this.body = body;
      this.statusCode = statusCode;
    }
  }
}
