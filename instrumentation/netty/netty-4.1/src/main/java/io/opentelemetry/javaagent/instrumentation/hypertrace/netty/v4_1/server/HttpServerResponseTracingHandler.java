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
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.AttributeKeys;
import io.opentelemetry.javaagent.instrumentation.netty.v4_1.server.NettyHttpServerTracer;
import java.nio.charset.Charset;
import java.util.Map;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.BoundedByteArrayOutputStreamFactory;
import org.hypertrace.agent.core.ContentLengthUtils;
import org.hypertrace.agent.core.ContentTypeCharsetUtils;
import org.hypertrace.agent.core.ContentTypeUtils;
import org.hypertrace.agent.core.HypertraceConfig;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;

public class HttpServerResponseTracingHandler extends ChannelOutboundHandlerAdapter {

  private final AgentConfig agentConfig = HypertraceConfig.get();

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise prm) {
    Context context = NettyHttpServerTracer.tracer().getServerContext(ctx.channel());
    if (context == null) {
      ctx.write(msg, prm);
      return;
    }
    Span span = Span.fromContext(context);

    if (msg instanceof HttpResponse) {
      HttpResponse httpResponse = (HttpResponse) msg;
      if (agentConfig.getDataCapture().getHttpHeaders().getResponse().getValue()) {
        captureHeaders(span, httpResponse);
      }

      CharSequence contentType = DataCaptureUtils.getContentType(httpResponse);
      if (agentConfig.getDataCapture().getHttpBody().getResponse().getValue()
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
        bufferAttr.set(BoundedByteArrayOutputStreamFactory.create(contentLength, charset));
      }
    }

    if (msg instanceof HttpContent
        && agentConfig.getDataCapture().getHttpBody().getResponse().getValue()) {
      DataCaptureUtils.captureBody(
          span, ctx.channel(), AttributeKeys.RESPONSE_BODY_BUFFER, (HttpContent) msg);
    }

    try (Scope ignored = context.makeCurrent()) {
      ctx.write(msg, prm);
    } catch (Throwable throwable) {
      NettyHttpServerTracer.tracer().endExceptionally(context, throwable);
      throw throwable;
    }
    if (msg instanceof HttpResponse) {
      HttpResponse httpResponse = (HttpResponse) msg;
      span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, httpResponse.status().code());
      span.setStatus(HttpStatusConverter.statusFromHttpStatus(httpResponse.status().code()));
    }
    if (msg instanceof FullHttpMessage || msg instanceof LastHttpContent) {
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
