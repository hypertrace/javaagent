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

package io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_0;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;

public class DataCaptureUtils {

  private DataCaptureUtils() {}

  public static void captureBody(
      Span span,
      Channel channel,
      AttributeKey<BoundedByteArrayOutputStream> attributeKey,
      Object httpContentOrBuffer) {

    Attribute<BoundedByteArrayOutputStream> bufferAttr = channel.attr(attributeKey);
    BoundedByteArrayOutputStream buffer = bufferAttr.get();
    if (buffer == null) {
      // not capturing body e.g. unknown content type
      return;
    }

    ByteBuf content = castToBuf(httpContentOrBuffer);
    if (content != null && content.isReadable()) {
      final ByteArrayOutputStream finalBuffer = buffer;
      content.forEachByte(
          value -> {
            finalBuffer.write(value);
            return true;
          });
    }

    if (httpContentOrBuffer instanceof LastHttpContent) {
      bufferAttr.remove();
      try {
        span.setAttribute(attributeKey.name(), buffer.toStringWithSuppliedCharset());
      } catch (UnsupportedEncodingException e) {
        // ignore charset was parsed before
      }
    }
  }

  private static ByteBuf castToBuf(Object msg) {
    if (msg instanceof ByteBuf) {
      return (ByteBuf) msg;
    } else if (msg instanceof HttpContent) {
      HttpContent httpContent = (HttpContent) msg;
      return httpContent.content();
    }
    return null;
  }

  // see io.netty.handler.codec.http.HttpUtil
  public static CharSequence getContentType(HttpMessage message) {
    return message.headers().get("content-type");
  }

  public static CharSequence getContentLength(HttpMessage message) {
    return message.headers().get("content-length");
  }
}
