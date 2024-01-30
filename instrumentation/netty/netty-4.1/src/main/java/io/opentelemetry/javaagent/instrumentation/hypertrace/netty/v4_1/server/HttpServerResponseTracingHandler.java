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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.Attribute;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.AttributeKeys;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.DataCaptureUtils;
import io.opentelemetry.javaagent.instrumentation.netty.v4_1.NettyServerSingletons;
import io.opentelemetry.semconv.SemanticAttributes;
import java.nio.charset.Charset;
import java.util.Map;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.utils.ContentLengthUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeCharsetUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;

public class HttpServerResponseTracingHandler extends ChannelOutboundHandlerAdapter {

  private static final InstrumentationConfig instrumentationConfig =
      InstrumentationConfig.ConfigProvider.get();

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise prm) {
    Context context =
        (Context)
            ctx.channel()
                .attr(
                    io.opentelemetry.instrumentation.netty.v4_1.internal.AttributeKeys
                        .SERVER_CONTEXT)
                .get();
    if (context == null) {
      ctx.write(msg, prm);
      return;
    }
    Span span = Span.fromContext(context);

    if (msg instanceof HttpResponse) {
      HttpResponse httpResponse = (HttpResponse) msg;
      if (instrumentationConfig.httpHeaders().response()) {
        captureHeaders(span, httpResponse);
      }

      CharSequence contentType = DataCaptureUtils.getContentType(httpResponse);
      if (instrumentationConfig.httpBody().response()
          && contentType != null
          && ContentTypeUtils.shouldCapture(contentType.toString())) {

        CharSequence contentLengthHeader = DataCaptureUtils.getContentLength(httpResponse);
        int contentLength = ContentLengthUtils.parseLength(contentLengthHeader);

        String charsetString = ContentTypeUtils.parseCharset(contentType.toString());
        Charset charset = ContentTypeCharsetUtils.toCharset(charsetString);

        // set the buffer to capture response body
        // the buffer is used byt captureBody method
        Attribute<BoundedByteArrayOutputStream> bufferAttr =
            ctx.channel().attr(AttributeKeys.RESPONSE_BODY_BUFFER);
        bufferAttr.set(BoundedBuffersFactory.createStream(contentLength, charset));
      }
    }

    if ((msg instanceof HttpContent || msg instanceof ByteBuf)
        && instrumentationConfig.httpBody().response()) {
      DataCaptureUtils.captureBody(span, ctx.channel(), AttributeKeys.RESPONSE_BODY_BUFFER, msg);
    }

    try (Scope ignored = context.makeCurrent()) {
      ctx.write(msg, prm);
    } catch (Throwable throwable) {
      NettyServerSingletons.instrumenter().end(context, null, null, throwable);
      throw throwable;
    }
    if (msg instanceof HttpResponse) {
      HttpResponse httpResponse = (HttpResponse) msg;
      int code = httpResponse.status().code();
      span.setAttribute(SemanticAttributes.HTTP_RESPONSE_STATUS_CODE, code);
      span.setStatus(code >= 100 && code < 500 ? StatusCode.UNSET : StatusCode.ERROR);
    }
    if (msg instanceof LastHttpContent) {
      // When we end the span, we should set the server context and request attr to null so that
      // for the next request a new context and request is made and stored in channel.
      // Else, when using a Connection: keep-alive header, the same server context and request attr
      // is used in the subsequent requests.
      ctx.channel()
          .attr(io.opentelemetry.instrumentation.netty.v4_1.internal.AttributeKeys.SERVER_CONTEXT)
          .set(null);
      ctx.channel().attr(AttributeKeys.REQUEST).set(null);
      span.end();
    }
  }

  private static void captureHeaders(Span span, HttpMessage httpMessage) {
    for (Map.Entry<String, String> entry : httpMessage.headers().entries()) {
      span.setAttribute(
          HypertraceSemanticAttributes.httpResponseHeader(entry.getKey()), entry.getValue());
    }
  }
}
