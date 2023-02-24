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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.AttributeKeys;
import java.util.Map;

import io.opentelemetry.javaagent.instrumentation.netty.common.HttpRequestAndChannel;
import org.hypertrace.agent.filter.FilterRegistry;

public class HttpServerBlockingRequestHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    Channel channel = ctx.channel();
    Context context =
        channel
            .attr(
                io.opentelemetry.javaagent.instrumentation.netty.v4_1.AttributeKeys.SERVER_CONTEXT)
            .get();
    if (context == null) {
      ctx.fireChannelRead(msg);
      return;
    }
    Span span = Span.fromContext(context);

    if (msg instanceof HttpRequest) {
      Attribute<Map<String, String>> headersAttr = channel.attr(AttributeKeys.REQUEST_HEADERS);
      Map<String, String> headers = headersAttr.getAndRemove();
      if (headers != null && FilterRegistry.getFilter().evaluateRequestHeaders(span, headers)) {
        forbidden(ctx, (HttpRequest) msg);
        return;
      }
    }
    if (msg instanceof HttpContent) {
      if (FilterRegistry.getFilter().evaluateRequestBody(span, null, null)) {
        Attribute<?> requestAttr = channel.attr(AttributeKeys.REQUEST);
        HttpRequest req = ((HttpRequestAndChannel)(requestAttr.get())).request();
        forbidden(ctx, req);
        return;
      }
    }
    ctx.fireChannelRead(msg);
  }

  static void forbidden(ChannelHandlerContext ctx, HttpRequest request) {
    DefaultFullHttpResponse blockResponse =
        new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.FORBIDDEN);
    blockResponse.headers().add("Content-Length", "0");
    ReferenceCountUtil.release(request);
    ctx.writeAndFlush(blockResponse).addListener(ChannelFutureListener.CLOSE);
  }
}
