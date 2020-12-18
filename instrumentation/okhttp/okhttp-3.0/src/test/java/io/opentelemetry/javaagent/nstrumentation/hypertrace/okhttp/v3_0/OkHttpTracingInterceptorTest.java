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

package io.opentelemetry.javaagent.nstrumentation.hypertrace.okhttp.v3_0;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.agent.testing.TestHttpServer;
import org.hypertrace.agent.testing.TestHttpServer.GetJsonHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OkHttpTracingInterceptorTest extends AbstractInstrumenterTest {

  private static final TestHttpServer testHttpServer = new TestHttpServer();

  private final OkHttpClient client = new OkHttpClient.Builder().followRedirects(true).build();

  @BeforeAll
  public static void startServer() throws Exception {
    testHttpServer.start();
  }

  @AfterAll
  public static void closeServer() throws Exception {
    testHttpServer.close();
  }

  @Test
  public void getNoContent() throws Exception {
    Request request =
        new Builder()
            .url(String.format("http://localhost:%d/get_no_content", testHttpServer.port()))
            .header("test-request-header", "test-value")
            .get()
            .build();

    Response response = client.newCall(request).execute();
    response.close();

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        "test-value",
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader("test-request-header")));
    Assertions.assertEquals(
        "test-value",
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader("test-response-header")));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void getJson() throws Exception {
    Request request =
        new Builder()
            .url(String.format("http://localhost:%d/get_json", testHttpServer.port()))
            .get()
            .build();

    Response response = client.newCall(request).execute();
    response.close();

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        "test-value",
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader("test-response-header")));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        GetJsonHandler.RESPONSE_BODY,
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void postUrlEncoded() throws Exception {
    FormBody formBody = new FormBody.Builder().add("key1", "value1").add("key2", "value2").build();
    Request request =
        new Builder()
            .url(String.format("http://localhost:%d/post", testHttpServer.port()))
            .post(formBody)
            .build();

    Response response = client.newCall(request).execute();
    response.close();

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        "test-value",
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader("test-response-header")));
    Assertions.assertEquals(
        "key1=value1&key2=value2",
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void postRedirect() throws Exception {
    String requestBodyStr = "{\"foo\": \"bar\"}";
    RequestBody requestBody = RequestBody.create(requestBodyStr, MediaType.get("application/json"));
    Request request =
        new Builder()
            .url(
                String.format(
                    "http://localhost:%d/post_redirect_to_get_no_content", testHttpServer.port()))
            .post(requestBody)
            .build();

    Response response = client.newCall(request).execute();
    response.close();

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData clientSpan = traces.get(0).get(0);
    Assertions.assertEquals(
        "test-value",
        clientSpan
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader("test-response-header")));
    Assertions.assertEquals(
        requestBodyStr,
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }
}
