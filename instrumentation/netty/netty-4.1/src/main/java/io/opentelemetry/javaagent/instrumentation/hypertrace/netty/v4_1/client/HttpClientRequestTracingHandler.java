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

package io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.AttributeKeys;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.DataCaptureUtils;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.utils.ContentLengthUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeCharsetUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;
import org.hypertrace.agent.otel.extensions.config.ServiceNameHeaderUtils;

public class HttpClientRequestTracingHandler extends ChannelOutboundHandlerAdapter {

  private static final InstrumentationConfig instrumentationConfig =
      InstrumentationConfig.ConfigProvider.get();

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise prm) {
    Channel channel = ctx.channel();
    Context context =
        channel
            .attr(io.opentelemetry.instrumentation.netty.v4_1.internal.AttributeKeys.CLIENT_CONTEXT)
            .get();
    if (context == null) {
      ctx.write(msg, prm);
      return;
    }
    Span span = Span.fromContext(context);

    if (msg instanceof HttpRequest) {
      HttpRequest httpRequest = (HttpRequest) msg;

      Map<String, String> headersMap = headersToMap(httpRequest);
      if (instrumentationConfig.httpHeaders().request()) {
        headersMap.forEach(span::setAttribute);
      }

      // Add service name header to outgoing requests
      httpRequest
          .headers()
          .add(
              ServiceNameHeaderUtils.getClientServiceKey(),
              ServiceNameHeaderUtils.getClientServiceName());

      CharSequence contentType = DataCaptureUtils.getContentType(httpRequest);
      if (instrumentationConfig.httpBody().request()
          && contentType != null
          && ContentTypeUtils.shouldCapture(contentType.toString())) {

        CharSequence contentLengthHeader = DataCaptureUtils.getContentLength(httpRequest);
        int contentLength = ContentLengthUtils.parseLength(contentLengthHeader);

        String charsetString = ContentTypeUtils.parseCharset(contentType.toString());
        Charset charset = ContentTypeCharsetUtils.toCharset(charsetString);

        // set the buffer to capture response body
        // the buffer is used byt captureBody method
        Attribute<BoundedByteArrayOutputStream> bufferAttr =
            ctx.channel().attr(AttributeKeys.REQUEST_BODY_BUFFER);
        bufferAttr.set(BoundedBuffersFactory.createStream(contentLength, charset));

        channel.attr(AttributeKeys.PROVIDED_CHARSET).set(charset);
      }
    }

    if ((msg instanceof HttpContent || msg instanceof ByteBuf)
        && instrumentationConfig.httpBody().request()) {
      Charset charset = channel.attr(AttributeKeys.PROVIDED_CHARSET).get();
      if (charset == null) {
        charset = ContentTypeCharsetUtils.getDefaultCharset();
      }
      DataCaptureUtils.captureBody(
          span, channel, AttributeKeys.REQUEST_BODY_BUFFER, msg, null, charset);
    }

    ctx.write(msg, prm);
  }

  private static Map<String, String> headersToMap(HttpMessage httpMessage) {
    Map<String, String> map = new HashMap<>();
    for (Map.Entry<String, String> entry : httpMessage.headers().entries()) {
      AttributeKey<String> key = HypertraceSemanticAttributes.httpRequestHeader(entry.getKey());
      map.put(key.getKey(), entry.getValue());
    }
    return map;
  }
}
