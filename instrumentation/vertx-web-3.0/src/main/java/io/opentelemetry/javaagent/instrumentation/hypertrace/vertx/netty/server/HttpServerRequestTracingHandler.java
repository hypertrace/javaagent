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

import static io.opentelemetry.javaagent.instrumentation.hypertrace.vertx.netty.server.HttpServerResponseTracingHandler.getContentLength;
import static io.opentelemetry.javaagent.instrumentation.hypertrace.vertx.netty.server.HttpServerResponseTracingHandler.getContentType;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.Attribute;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.hypertrace.vertx.netty.AttributeKeys;
import io.opentelemetry.javaagent.instrumentation.netty.v4_0.server.NettyHttpServerTracer;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import org.hypertrace.agent.core.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.BoundedByteArrayOutputStreamFactory;
import org.hypertrace.agent.core.ContentLengthUtils;
import org.hypertrace.agent.core.ContentTypeCharsetUtils;
import org.hypertrace.agent.core.ContentTypeUtils;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerRequestTracingHandler extends ChannelInboundHandlerAdapter {

  private static final Logger log = LoggerFactory.getLogger(HttpServerResponseTracingHandler.class);

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
      HttpMessage httpMessage = (HttpMessage) msg;
      // TODO add blocking
      // TODO maybe block once the full body is retrieved
      //      if (true) {
      //        DefaultFullHttpResponse blockResponse = new DefaultFullHttpResponse(
      //            httpMessage.getProtocolVersion(), HttpResponseStatus.FORBIDDEN);
      ////        blockResponse.headers().add("Connection",  "Keep-Alive");
      //        ctx.writeAndFlush(blockResponse)
      //            .addListener(ChannelFutureListener.CLOSE);
      //        ReferenceCountUtil.release(msg);
      //        return;
      //      }

      CharSequence contentType = getContentType(httpMessage);
      if (contentType != null && ContentTypeUtils.shouldCapture(contentType.toString())) {
        CharSequence contentLengthHeader = getContentLength(httpMessage);
        int contentLength = ContentLengthUtils.parseLength(contentLengthHeader);

        String charsetString = ContentTypeUtils.parseCharset(contentType.toString());
        Charset charset = ContentTypeCharsetUtils.toCharset(charsetString);

        // set the buffer to capture response body
        // the buffer is used byt captureBody method
        Attribute<BoundedByteArrayOutputStream> bufferAttr =
            ctx.channel().attr(AttributeKeys.REQUEST_BODY_BUFFER);
        bufferAttr.set(BoundedByteArrayOutputStreamFactory.create(contentLength, charset));
      }

      processHttpMessage(span, httpMessage);
    }

    if (msg instanceof HttpContent) {
      captureBody(span, channel, (HttpContent) msg);
    }

    ctx.fireChannelRead(msg);
  }

  private static void processHttpMessage(Span span, HttpMessage httpMessage) {
    for (Map.Entry<String, String> entry : httpMessage.headers().entries()) {
      span.setAttribute(
          HypertraceSemanticAttributes.httpRequestHeader(entry.getKey()), entry.getValue());
    }
  }

  private static void captureBody(Span span, Channel channel, HttpContent httpContent) {
    Attribute<BoundedByteArrayOutputStream> bufferAttr =
        channel.attr(AttributeKeys.REQUEST_BODY_BUFFER);
    BoundedByteArrayOutputStream buffer = bufferAttr.get();
    if (buffer == null) {
      // not capturing body e.g. unknown content type
      return;
    }

    System.out.println(httpContent.getClass().getName());
    System.out.println(httpContent.content().getClass().getName());
    System.out.println(httpContent.content().capacity());

    final ByteArrayOutputStream finalBuffer = buffer;
    httpContent
        .content()
        .forEachByte(
            value -> {
              finalBuffer.write(value);
              System.out.printf("request content byte: %s\n", (char) value);
              return true;
            });

    if (httpContent instanceof LastHttpContent) {
      System.out.println("It is the last content");
      // TODO set encoding
      bufferAttr.remove();
      try {
        span.setAttribute(
            HypertraceSemanticAttributes.HTTP_REQUEST_BODY, buffer.toStringWithSuppliedCharset());
      } catch (UnsupportedEncodingException e) {
        // ignore charset was parsed before
      }
    }
  }
}
