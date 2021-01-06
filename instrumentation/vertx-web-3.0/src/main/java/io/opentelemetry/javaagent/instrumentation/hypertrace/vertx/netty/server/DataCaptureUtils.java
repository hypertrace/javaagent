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
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import org.hypertrace.agent.core.BoundedByteArrayOutputStream;

public class DataCaptureUtils {

  private DataCaptureUtils() {}

  public static void captureBody(
      Span span,
      Channel channel,
      AttributeKey<BoundedByteArrayOutputStream> attributeKey,
      HttpContent httpContent) {

    Attribute<BoundedByteArrayOutputStream> bufferAttr = channel.attr(attributeKey);
    BoundedByteArrayOutputStream buffer = bufferAttr.get();
    if (buffer == null) {
      // not capturing body e.g. unknown content type
      return;
    }

    final ByteArrayOutputStream finalBuffer = buffer;
    httpContent
        .content()
        .forEachByte(
            value -> {
              finalBuffer.write(value);
              return true;
            });

    if (httpContent instanceof LastHttpContent) {
      bufferAttr.remove();
      try {
        span.setAttribute(attributeKey.name(), buffer.toStringWithSuppliedCharset());
      } catch (UnsupportedEncodingException e) {
        // ignore charset was parsed before
      }
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
