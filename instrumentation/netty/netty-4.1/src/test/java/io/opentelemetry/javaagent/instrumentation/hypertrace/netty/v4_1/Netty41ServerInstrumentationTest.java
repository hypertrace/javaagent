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

package io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Netty41ServerInstrumentationTest extends AbstractInstrumenterTest {

  private static final LoggingHandler LOGGING_HANDLER =
      new LoggingHandler(Netty41ServerInstrumentationTest.class, LogLevel.DEBUG);

  public static final String REQUEST_HEADER_NAME = "reqheader";
  public static final String REQUEST_HEADER_VALUE = "reqheadervalue";
  public static final String RESPONSE_HEADER_NAME = "respheader";
  public static final String RESPONSE_HEADER_VALUE = "respheadervalue";
  private static final String RESPONSE_BODY = "{\"foo\": \"bar\"}";

  private static EventLoopGroup eventLoopGroup;
  private static int port;

  @BeforeAll
  private static void startServer() throws IOException, InterruptedException {
    eventLoopGroup = new NioEventLoopGroup();

    ServerBootstrap serverBootstrap = new ServerBootstrap();
    serverBootstrap.group(eventLoopGroup);
    serverBootstrap
        .handler(LOGGING_HANDLER)
        .childHandler(
            new ChannelInitializer<Channel>() {
              @Override
              protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addFirst("logger", LOGGING_HANDLER);

                pipeline.addLast(new HttpRequestDecoder());
                pipeline.addLast(new HttpResponseEncoder());

                pipeline.addLast(
                    new SimpleChannelInboundHandler() {
                      @Override
                      protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
                        // write response after all content has been received otherwise
                        // server span is closed before request payload is captured
                        if (msg instanceof LastHttpContent) {
                          ByteBuf responseBody = Unpooled.wrappedBuffer(RESPONSE_BODY.getBytes());
                          HttpResponse response =
                              new DefaultFullHttpResponse(
                                  HTTP_1_1, HttpResponseStatus.valueOf(200), responseBody);
                          response.headers().add(RESPONSE_HEADER_NAME, RESPONSE_HEADER_VALUE);
                          response.headers().add("Content-Type", "application-json");
                          response.headers().set(CONTENT_LENGTH, responseBody.readableBytes());
                          ctx.write(response);
                        }
                      }

                      @Override
                      public void channelReadComplete(ChannelHandlerContext ctx) {
                        ctx.flush();
                      }
                    });
              }
            })
        .channel(NioServerSocketChannel.class);

    ServerSocket socket;
    socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();

    serverBootstrap.bind(port).sync();
  }

  @AfterAll
  private static void stopServer() {
    if (eventLoopGroup != null) {
      eventLoopGroup.shutdownGracefully();
    }
  }

  @Test
  public void postJson() throws IOException, TimeoutException, InterruptedException {
    RequestBody requestBody = requestBody(true, 3000, 75);
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d", port))
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
            .url(String.format("http://localhost:%d", port))
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
  }

  private RequestBody requestBody(final boolean chunked, final long size, final int writeSize) {
    final byte[] buffer = new byte[writeSize];
    Arrays.fill(buffer, (byte) 'x');

    return new RequestBody() {
      @Override
      public MediaType contentType() {
        return MediaType.get("application/json; charset=utf-8");
      }

      @Override
      public long contentLength() throws IOException {
        return chunked ? -1L : size;
      }

      @Override
      public void writeTo(BufferedSink sink) throws IOException {
        for (int count = 0; count < size; count += writeSize) {
          sink.write(buffer, 0, (int) Math.min(size - count, writeSize));
        }
      }
    };
  }
}
