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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.AttributeKeys;
import io.opentelemetry.javaagent.instrumentation.netty.v4_1.server.NettyHttpServerTracer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.config.HypertraceConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.utils.ContentLengthUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeCharsetUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;

public class HttpServerRequestTracingHandler extends ChannelInboundHandlerAdapter {

  private final AgentConfig agentConfig = HypertraceConfig.get();

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
      HttpRequest httpRequest = (HttpRequest) msg;

      Map<String, String> headersMap = headersToMap(httpRequest);
      if (agentConfig.getDataCapture().getHttpHeaders().getRequest().getValue()) {
        headersMap.forEach((key, value) -> span.setAttribute(key, value));
      }
      // used by blocking handler
      channel.attr(AttributeKeys.REQUEST_HEADERS).set(headersMap);

      CharSequence contentType = DataCaptureUtils.getContentType(httpRequest);
      if (agentConfig.getDataCapture().getHttpBody().getRequest().getValue()
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
      }
    }

    if (msg instanceof HttpContent
        && agentConfig.getDataCapture().getHttpBody().getRequest().getValue()) {
      DataCaptureUtils.captureBody(
          span, channel, AttributeKeys.REQUEST_BODY_BUFFER, (HttpContent) msg);
    }

    ctx.fireChannelRead(msg);
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
