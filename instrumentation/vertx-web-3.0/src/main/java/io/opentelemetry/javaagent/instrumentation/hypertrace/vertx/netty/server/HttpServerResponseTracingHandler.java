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

import static io.opentelemetry.javaagent.instrumentation.netty.v4_0.server.NettyHttpServerTracer.tracer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.Attribute;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.attributes.SemanticAttributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.tracer.HttpStatusConverter;
import io.opentelemetry.javaagent.instrumentation.hypertrace.vertx.netty.AttributeKeys;
import io.opentelemetry.javaagent.instrumentation.netty.v4_0.server.NettyHttpServerTracer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

public class HttpServerResponseTracingHandler extends ChannelOutboundHandlerAdapter {

  private static final Logger log = LoggerFactory.getLogger(HttpServerResponseTracingHandler.class);

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise prm) {
    System.out.printf("calling write, msg: %s\n", msg.getClass());
    Context context = NettyHttpServerTracer.tracer().getServerContext(ctx.channel());
    if (context == null) {
      ctx.write(msg, prm);
      return;
    }
    Span span = Span.fromContext(context);

    if (msg instanceof HttpMessage) {
      HttpMessage httpMessage = (HttpMessage) msg;
      captureHeaders(span, httpMessage);

      CharSequence contentType = getContentType(httpMessage);
      if (contentType != null && ContentTypeUtils.shouldCapture(contentType.toString())) {
        CharSequence contentLengthHeader = getContentLength(httpMessage);
        int contentLength = ContentLengthUtils.parseLength(contentLengthHeader);

        String charsetString = ContentTypeUtils.parseCharset(contentType.toString());
        Charset charset = ContentTypeCharsetUtils.toCharset(charsetString);

        // set the buffer to capture response body
        // the buffer is used byt captureBody method
        Attribute<BoundedByteArrayOutputStream> bufferAttr =
            ctx.channel().attr(AttributeKeys.RESPONSE_BODY_BUFFER);
        bufferAttr.set(BoundedByteArrayOutputStreamFactory.create(contentLength, charset));
      }
    }

    if (msg instanceof HttpContent) {
      captureBody(span, ctx.channel(), (HttpContent) msg);
    }

    try (Scope ignored = context.makeCurrent()) {
      ctx.write(msg, prm);
    } catch (Throwable throwable) {
      tracer().endExceptionally(context, throwable);
      throw throwable;
    }
    if (msg instanceof HttpResponse) {
      HttpResponse httpResponse = (HttpResponse) msg;
      span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, httpResponse.getStatus().code());
      span.setStatus(HttpStatusConverter.statusFromHttpStatus(httpResponse.getStatus().code()));
    }
    if (msg instanceof FullHttpMessage || msg instanceof LastHttpContent) {
      span.end();
    }
  }

  private static void captureBody(Span span, Channel channel, HttpContent httpContent) {
    Attribute<BoundedByteArrayOutputStream> bufferAttr =
        channel.attr(AttributeKeys.RESPONSE_BODY_BUFFER);
    BoundedByteArrayOutputStream buffer = bufferAttr.get();
    if (buffer == null) {
      // not capturing body e.g. unknown content type
      return;
    }

    System.out.println(httpContent.content().getClass().getName());

    System.out.println(httpContent.content().capacity());
    if (httpContent.content().hasArray()) {
      byte[] array = httpContent.content().array();
      try {
        buffer.write(array);
      } catch (IOException e) {
        log.error("Failed to write body array to buffer", e);
      }
      System.out.printf("response content array: %s\n", new String(array));
    } else {
      final ByteArrayOutputStream finalBuffer = buffer;
      httpContent
          .content()
          .forEachByte(
              value -> {
                finalBuffer.write(value);
                return true;
              });
      System.out.printf(
          "response content forEachByte: %s\n", new String(httpContent.content().array()));
    }

    if (httpContent instanceof LastHttpContent) {
      System.out.println("It is the last content");
      // TODO set encoding
      bufferAttr.remove();
      try {
        span.setAttribute(
            HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, buffer.toStringWithSuppliedCharset());
      } catch (UnsupportedEncodingException e) {
        // charset was parsed before
      }
    }
  }

  private static void captureHeaders(Span span, HttpMessage httpMessage) {
    System.out.println("It is http message");
    for (Map.Entry<String, String> entry : httpMessage.headers().entries()) {
      span.setAttribute(
          HypertraceSemanticAttributes.httpResponseHeader(entry.getKey()), entry.getValue());
    }
  }

  // see io.netty.handler.codec.http.HttpUtil
  public static CharSequence getContentType(HttpMessage message) {
    return message.headers().get("content-type");
  }

  public static CharSequence getContentLength(HttpMessage message) {
    return message.headers().get("content-length");
  }
}
