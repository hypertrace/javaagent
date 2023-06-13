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

package io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.server;

import static io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.server.NettyTestServer.RESPONSE_BODY;
import static io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.server.NettyTestServer.RESPONSE_HEADER_NAME;
import static io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.server.NettyTestServer.RESPONSE_HEADER_VALUE;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractNetty41ServerInstrumentationTest extends AbstractInstrumenterTest {

  public static final String REQUEST_HEADER_NAME = "reqheader";
  public static final String REQUEST_HEADER_VALUE = "reqheadervalue";

  private static int port;
  private static NettyTestServer nettyTestServer;

  @BeforeEach
  private void startServer() throws IOException, InterruptedException {
    nettyTestServer = createNetty();
    port = nettyTestServer.create();
  }

  @AfterEach
  private void stopServer() throws ExecutionException, InterruptedException {
    nettyTestServer.stopServer();
  }

  protected abstract NettyTestServer createNetty();

  @Test
  public void get() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/get_no_content", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .get()
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(204, response.code());
    }

    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<SpanData> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    SpanData spanData = trace.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertEquals(
        RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(RESPONSE_HEADER_NAME)));
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void postJson() throws IOException, TimeoutException, InterruptedException {
    RequestBody requestBody = requestBody(true, 3000, 75);
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/post", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .header("Transfer-Encoding", "chunked")
            .post(requestBody)
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
      Assertions.assertEquals(RESPONSE_BODY, response.body().string());
    }

    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<SpanData> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    SpanData spanData = trace.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertEquals(
        RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(RESPONSE_HEADER_NAME)));
    Buffer requestBodyBuffer = new Buffer();
    requestBody.writeTo(requestBodyBuffer);
    Assertions.assertEquals(
        new String(requestBodyBuffer.readByteArray()),
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        RESPONSE_BODY,
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void blocking() throws IOException, TimeoutException, InterruptedException {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/post", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .header("mockblock", "true")
            .get()
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(403, response.code());
      Assertions.assertTrue(response.body().string().isEmpty());
    }

    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<SpanData> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    SpanData spanData = trace.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertNull(
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(RESPONSE_HEADER_NAME)));
    Assertions.assertNull(
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(RESPONSE_BODY)));

    RequestBody requestBody = blockedRequestBody(true, 3000, 75);
    Request request2 =
        new Request.Builder()
            .url(String.format("http://localhost:%d/post", port))
            .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
            .post(requestBody)
            .build();

    try (Response response = httpClient.newCall(request2).execute()) {
      Assertions.assertEquals(403, response.code());
      Assertions.assertTrue(response.body().string().isEmpty());
    }

    List<List<SpanData>> traces2 = TEST_WRITER.getTraces();
    TEST_WRITER.waitForTraces(2);
    Assertions.assertEquals(2, traces2.size());
    List<SpanData> trace2 = traces2.get(1);
    Assertions.assertEquals(1, trace2.size());
    SpanData spanData2 = trace2.get(0);

    Assertions.assertEquals(
        REQUEST_HEADER_VALUE,
        spanData2
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertNull(
        spanData2
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(RESPONSE_HEADER_NAME)));
    Assertions.assertNull(
        spanData2
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(RESPONSE_BODY)));
  }

  @Test
  public void connectionKeepAlive() throws IOException, TimeoutException, InterruptedException {
    Request request =
            new Request.Builder()
                    .url(String.format("http://localhost:%d/post", port))
                    .header(REQUEST_HEADER_NAME, REQUEST_HEADER_VALUE)
                    .header("first", "1st")
                    .header("connection", "keep-alive")
                    .get()
                    .build();

    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
      Assertions.assertEquals(RESPONSE_BODY, response.body().string());
    }

    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<SpanData> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    SpanData spanData = trace.get(0);

    Assertions.assertEquals(
            REQUEST_HEADER_VALUE,
            spanData
                    .getAttributes()
                    .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertEquals(
            "1st",
            spanData
                    .getAttributes()
                    .get(HypertraceSemanticAttributes.httpRequestHeader("first")));
    Assertions.assertEquals(
            "keep-alive",
            spanData
                    .getAttributes()
                    .get(HypertraceSemanticAttributes.httpRequestHeader("connection")));
    Assertions.assertEquals(
            RESPONSE_HEADER_VALUE,
            spanData
                    .getAttributes()
                    .get(HypertraceSemanticAttributes.httpResponseHeader(RESPONSE_HEADER_NAME)));
    Assertions.assertNull(
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
            RESPONSE_BODY,
            spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));


    RequestBody requestBody = blockedRequestBody(true, 3000, 75);
    Request request2 =
            new Request.Builder()
                    .url(String.format("http://localhost:%d/post", port))
                    .header(REQUEST_HEADER_NAME, "REQUEST_HEADER_VALUE")
                    .header("second", "2nd")
                    .header("connection", "keep-alive")
                    .post(requestBody)
                    .build();

    try (Response response = httpClient.newCall(request2).execute()) {
      Assertions.assertEquals(403, response.code());
      Assertions.assertTrue(response.body().string().isEmpty());
    }

    List<List<SpanData>> traces2 = TEST_WRITER.getTraces();
    TEST_WRITER.waitForTraces(2);
    Assertions.assertEquals(2, traces2.size());
    List<SpanData> trace2 = traces2.get(1);
    Assertions.assertEquals(1, trace2.size());
    SpanData spanData2 = trace2.get(0);

    Assertions.assertEquals(
            "REQUEST_HEADER_VALUE",
            spanData2
                    .getAttributes()
                    .get(HypertraceSemanticAttributes.httpRequestHeader(REQUEST_HEADER_NAME)));
    Assertions.assertEquals(
            "2nd",
            spanData2
                    .getAttributes()
                    .get(HypertraceSemanticAttributes.httpRequestHeader("second")));
    Assertions.assertEquals(
            "keep-alive",
            spanData2
                    .getAttributes()
                    .get(HypertraceSemanticAttributes.httpRequestHeader("connection")));
    Assertions.assertNull(
            spanData2
                    .getAttributes()
                    .get(HypertraceSemanticAttributes.httpRequestHeader("first")));
    Assertions.assertNull(
            spanData2
                    .getAttributes()
                    .get(HypertraceSemanticAttributes.httpResponseHeader(RESPONSE_HEADER_NAME)));
    Assertions.assertNull(
            spanData2
                    .getAttributes()
                    .get(HypertraceSemanticAttributes.httpResponseHeader(RESPONSE_BODY)));
  }
}
