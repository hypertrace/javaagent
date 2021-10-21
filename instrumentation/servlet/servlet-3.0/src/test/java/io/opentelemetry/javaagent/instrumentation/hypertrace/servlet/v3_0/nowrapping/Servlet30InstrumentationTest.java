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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping;

import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.TestServlets.EchoAsyncResponse_stream;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.TestServlets.EchoAsyncResponse_writer;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.TestServlets.EchoReader_read_large_array;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.TestServlets.EchoStream_arr;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.TestServlets.EchoStream_arr_offset;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.TestServlets.EchoStream_readLine_print;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.TestServlets.EchoStream_read_large_array;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.TestServlets.EchoStream_single_byte;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.TestServlets.EchoWriter_single_char;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.TestServlets.GetHello;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.DispatcherType;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.WrappingFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Servlet30InstrumentationTest extends AbstractInstrumenterTest {
  private static final String REQUEST_BODY = "hello";
  private static final String REQUEST_HEADER = "requestheader";
  private static final String REQUEST_HEADER_VALUE = "requestvalue";

  private static Server server = new Server(0);
  private static int serverPort;

  @BeforeAll
  public static void startServer() throws Exception {
    ServletContextHandler handler = new ServletContextHandler();

    handler.addFilter(WrappingFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

    handler.addServlet(GetHello.class, "/hello");
    handler.addServlet(EchoStream_single_byte.class, "/echo_stream_single_byte");
    handler.addServlet(EchoStream_arr.class, "/echo_stream_arr");
    handler.addServlet(EchoStream_arr_offset.class, "/echo_stream_arr_offset");
    handler.addServlet(EchoStream_readLine_print.class, "/echo_stream_readLine_print");
    handler.addServlet(EchoWriter_single_char.class, "/echo_writer_single_char");
    handler.addServlet(TestServlets.EchoWriter_arr.class, "/echo_writer_arr");
    handler.addServlet(TestServlets.EchoWriter_arr_offset.class, "/echo_writer_arr_offset");
    handler.addServlet(TestServlets.EchoWriter_readLine_write.class, "/echo_writer_readLine_write");
    handler.addServlet(TestServlets.EchoWriter_readLines.class, "/echo_writer_readLines");
    handler.addServlet(
        TestServlets.EchoWriter_readLine_print_str.class, "/echo_writer_readLine_print_str");
    handler.addServlet(
        TestServlets.EchoWriter_readLine_print_arr.class, "/echo_writer_readLine_print_arr");
    handler.addServlet(TestServlets.Forward_to_post.class, "/forward_to_echo");
    handler.addServlet(EchoAsyncResponse_stream.class, "/echo_async_response_stream");
    handler.addServlet(EchoAsyncResponse_writer.class, "/echo_async_response_writer");
    handler.addServlet(EchoStream_read_large_array.class, "/echo_stream_read_large_array");
    handler.addServlet(EchoReader_read_large_array.class, "/echo_reader_read_large_array");
    server.setHandler(handler);
    server.start();
    serverPort = server.getConnectors()[0].getLocalPort();
  }

  @AfterAll
  public static void stopServer() throws Exception {
    server.stop();
  }

  @Test
  public void forward_to_post() throws Exception {
    postJson(String.format("http://localhost:%d/forward_to_echo", serverPort));
  }

  @Test
  public void echo_async_response_stream() throws Exception {
    postJson(String.format("http://localhost:%d/echo_async_response_stream", serverPort));
  }

  @Test
  public void echo_async_response_writer() throws Exception {
    postJson(String.format("http://localhost:%d/echo_async_response_writer", serverPort));
  }

  @Test
  public void postJson_stream_single_byte() throws Exception {
    postJson(String.format("http://localhost:%d/echo_stream_single_byte", serverPort));
  }

  @Test
  public void postJson_stream_read_large_array() throws Exception {
    postJson(String.format("http://localhost:%d/echo_stream_read_large_array", serverPort));
  }

  @Test
  public void postJson_reader_read_large_array() throws Exception {
    postJson(String.format("http://localhost:%d/echo_reader_read_large_array", serverPort));
  }

  @Test
  public void postJson_stream_arr() throws Exception {
    postJson(String.format("http://localhost:%d/echo_stream_arr", serverPort));
  }

  @Test
  public void postJson_stream_arr_offset() throws Exception {
    postJson(String.format("http://localhost:%d/echo_stream_arr_offset", serverPort));
  }

  @Test
  public void postJson_stream_readLine_print() throws Exception {
    postJson(String.format("http://localhost:%d/echo_stream_readLine_print", serverPort));
  }

  @Test
  public void postJson_writer_single_char() throws Exception {
    postJson(String.format("http://localhost:%d/echo_writer_single_char", serverPort));
  }

  @Test
  public void postJson_writer_arr() throws Exception {
    postJson(String.format("http://localhost:%d/echo_writer_arr", serverPort));
  }

  @Test
  public void postJson_writer_arr_offset() throws Exception {
    postJson(String.format("http://localhost:%d/echo_writer_arr_offset", serverPort));
  }

  @Test
  public void postJson_writer_readLine_write() throws Exception {
    postJson(String.format("http://localhost:%d/echo_writer_readLine_write", serverPort));
  }

  @Test
  public void postJson_writer_readLine_print_str() throws Exception {
    postJson(String.format("http://localhost:%d/echo_writer_readLine_print_str", serverPort));
  }

  @Test
  public void postJson_writer_readLines() throws Exception {
    postJson(String.format("http://localhost:%d/echo_writer_readLines", serverPort));
  }

  @Test
  public void postJson_writer_readLine_print_arr() throws Exception {
    postJson(String.format("http://localhost:%d/echo_writer_readLine_print_arr", serverPort));
  }

  @Test
  public void portUrlEncoded() throws Exception {
    FormBody formBody = new FormBody.Builder().add("key1", "value1").add("key2", "value2").build();
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/echo_stream_single_byte", serverPort))
            .post(formBody)
            .header(REQUEST_HEADER, REQUEST_HEADER_VALUE)
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
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
        TestServlets.RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(TestServlets.RESPONSE_HEADER)));
    Assertions.assertEquals(
        "key1=value1&key2=value2",
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        TestServlets.RESPONSE_BODY,
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void getHello() throws Exception {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/hello", serverPort))
            .get()
            .header(REQUEST_HEADER, REQUEST_HEADER_VALUE)
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(204, response.code());
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
        TestServlets.RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(TestServlets.RESPONSE_HEADER)));
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void block() throws Exception {
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/hello", serverPort))
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
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(TestServlets.RESPONSE_HEADER)));
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  @Test
  public void blockBody() throws Exception {
    FormBody formBody = new FormBody.Builder().add("block", "true").build();
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/echo_stream_single_byte", serverPort))
            .post(formBody)
            .header(REQUEST_HEADER, REQUEST_HEADER_VALUE)
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
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(TestServlets.RESPONSE_HEADER)));
    Assertions.assertEquals(
        "block=true", spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertNull(
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }

  public void postJson(String url) throws Exception {
    Request request =
        new Request.Builder()
            .url(url)
            .post(RequestBody.create(REQUEST_BODY, MediaType.get("application/json")))
            .header(REQUEST_HEADER, REQUEST_HEADER_VALUE)
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      Assertions.assertEquals(200, response.code());
      Assertions.assertEquals(TestServlets.RESPONSE_BODY, response.body().string());
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
        TestServlets.RESPONSE_HEADER_VALUE,
        spanData
            .getAttributes()
            .get(HypertraceSemanticAttributes.httpResponseHeader(TestServlets.RESPONSE_HEADER)));
    Assertions.assertEquals(
        REQUEST_BODY, spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(
        TestServlets.RESPONSE_BODY,
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
  }
}
