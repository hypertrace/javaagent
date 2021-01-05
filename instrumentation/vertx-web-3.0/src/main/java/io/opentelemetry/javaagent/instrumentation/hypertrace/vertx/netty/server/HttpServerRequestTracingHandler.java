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

package io.opentelemetry.javaagent.instrumentation.hypertrace.vertx.netty.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.netty.v4_0.server.NettyHttpServerTracer;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;

public class HttpServerRequestTracingHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    Channel channel = ctx.channel();

    Context context = NettyHttpServerTracer.tracer().getServerContext(channel);
    if (context == null) {
      ctx.fireChannelRead(msg);
      return;
    }
    Span span = Span.fromContext(context);

    if (msg instanceof HttpRequest) {
      processHttpMessage(span, (HttpMessage) msg);
    }

    if (msg instanceof HttpContent) {
      processHttpContent(span, (HttpContent) msg);
    }

    ctx.fireChannelRead(msg);
  }

  private static void processHttpMessage(Span span, HttpMessage httpMessage) {
    for (Map.Entry<String, String> entry : httpMessage.headers().entries()) {
      span.setAttribute(
          HypertraceSemanticAttributes.httpRequestHeader(entry.getKey()), entry.getValue());
    }

    if (httpMessage instanceof FullHttpMessage) {
      System.out.println("it is full request message\n\n\n");
      FullHttpMessage fullMessage = (FullHttpMessage) httpMessage;
      System.out.printf("Request Body: %s", new String(fullMessage.content().array()));
    }
  }

  private static void processHttpContent(Span span, HttpContent httpContent) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    httpContent
        .content()
        .forEachByte(
            value -> {
              buffer.write(value);
              return true;
            });

    // TODO check chunked body
    System.out.printf(
        "Captured request body: %s, span is recording: %s\n",
        new String(buffer.toByteArray()), span.isRecording());
    span.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, buffer.toString());
  }
}
