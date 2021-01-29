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

public abstract class AbstractHttpClientTest extends AbstractInstrumenterTest {

  protected static final TestHttpServer testHttpServer = new TestHttpServer();

  public static final String GET_NO_CONTENT_PATH = "http://localhost:%d/get_no_content";

  public static final String GET_JSON_PATH = "http://localhost:%d/get_json";

  public static final String POST_PATH = "http://localhost:%d/post";

  public static final String POST_REDIRECT_PATH =
      "http://localhost:%d/post_redirect_to_get_no_content";

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
   * Test for post request with json body. Use ECHO_PATH as url string
   */
  public abstract void echoJson() throws Exception;

  /**
   * Test for post request with url encoded form body. Use ECHO_PATH as url string
   */
  public abstract void echoUrlEncoded() throws Exception;

  /**
   * Test for post request with plain text body. Use ECHO_PATH as url string
   */
  public abstract void echoPlainText() throws Exception;

  public void assertResponseHeaders(SpanData spanData) {
    Assertions.assertEquals(
        TestHttpServer.RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(
                HypertraceSemanticAttributes.httpResponseHeader(
                    TestHttpServer.RESPONSE_HEADER_NAME)));
  }

  public void assertGetJsonResponseBody(SpanData spanData) {
    Assertions.assertEquals(
        TestHttpServer.GetJsonHandler.RESPONSE_BODY,
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  public void assertEchoJson(SpanData spanData) {
    assertResponseHeaders(spanData);
    Assertions.assertNotNull(spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    Assertions.assertEquals(
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY),
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
  }

  public void assertEchoUrlEncoded(SpanData spanData) {
    assertResponseHeaders(spanData);
    Assertions.assertNotNull(spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    Assertions.assertEquals(
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY),
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
  }

  public void assertEchoPlainText(SpanData spanData) {
    assertResponseHeaders(spanData);
    Assertions.assertNull(
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    Assertions.assertNull(
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
  }

}
