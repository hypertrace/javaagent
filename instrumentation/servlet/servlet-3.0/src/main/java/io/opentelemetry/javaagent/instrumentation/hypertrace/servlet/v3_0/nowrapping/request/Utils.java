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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.request;

import io.opentelemetry.api.trace.Span;
import java.nio.charset.Charset;
import javax.servlet.http.HttpServletRequest;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.ByteBufferSpanPair;
import org.hypertrace.agent.core.instrumentation.buffer.CharBufferSpanPair;
import org.hypertrace.agent.core.instrumentation.utils.ContentLengthUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeCharsetUtils;

public class Utils {

  private Utils() {}

  public static ByteBufferSpanPair createRequestByteBufferSpanPair(
      HttpServletRequest httpServletRequest, Span span) {
    String charsetStr = httpServletRequest.getCharacterEncoding();
    Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);
    int contentLength = httpServletRequest.getContentLength();
    if (contentLength < 0) {
      contentLength = ContentLengthUtils.DEFAULT;
    }
    return new ByteBufferSpanPair(span, BoundedBuffersFactory.createStream(contentLength, charset));
  }

  public static CharBufferSpanPair createRequestCharBufferSpanPair(
      HttpServletRequest httpServletRequest, Span span) {
    int contentLength = httpServletRequest.getContentLength();
    if (contentLength < 0) {
      contentLength = ContentLengthUtils.DEFAULT;
    }
    return new CharBufferSpanPair(span, BoundedBuffersFactory.createWriter(contentLength));
  }
}
