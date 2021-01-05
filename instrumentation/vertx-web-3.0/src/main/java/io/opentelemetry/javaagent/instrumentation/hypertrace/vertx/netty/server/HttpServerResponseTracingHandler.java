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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.netty.v4_0.server.NettyHttpServerTracer;
import java.util.Map;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;

public class HttpServerResponseTracingHandler extends ChannelOutboundHandlerAdapter {

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise prm) {
    System.out.println("calling write");
    Context context = NettyHttpServerTracer.tracer().getServerContext(ctx.channel());
    if (context == null || !(msg instanceof HttpResponse)) {
      System.out.println(context);
      System.out.println(msg);
      ctx.write(msg, prm);
      return;
    }

    System.out.println("It is http message");
    HttpMessage httpMessage = (HttpMessage) msg;
    Span span = Span.fromContext(context);

    for (Map.Entry<String, String> entry : httpMessage.headers().entries()) {
      span.setAttribute(
          HypertraceSemanticAttributes.httpResponseHeader(entry.getKey()), entry.getValue());
    }

    if (msg instanceof FullHttpMessage) {
      System.out.println("it if full response message\n\n\n");
      FullHttpMessage fullmessage = (FullHttpMessage) msg;
      // TODO CHECK COntet type and boDY size
      span.setAttribute(
          HypertraceSemanticAttributes.HTTP_RESPONSE_BODY,
          new String(fullmessage.content().array()));
      System.out.printf("Response Body: %s\n", new String(fullmessage.content().array()));
    }
    //    AssembledHttpResponse

    ctx.write(msg, prm);
  }
}
