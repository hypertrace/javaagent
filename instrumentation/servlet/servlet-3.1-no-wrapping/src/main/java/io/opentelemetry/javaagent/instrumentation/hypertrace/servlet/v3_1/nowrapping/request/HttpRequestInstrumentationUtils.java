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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.request;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.ByteBufferMetadata;
import java.nio.charset.Charset;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.CharBufferAndSpan;
import org.hypertrace.agent.core.instrumentation.utils.ContentLengthUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeCharsetUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;

public class HttpRequestInstrumentationUtils {

  private HttpRequestInstrumentationUtils() {}

  public static ByteBufferMetadata createRequestByteBufferMetadata(
      HttpServletRequest httpServletRequest, Span span) {
    String contentType = httpServletRequest.getContentType();
    String charsetStr = ContentTypeUtils.parseCharset(contentType);
    Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);
    int contentLength = httpServletRequest.getContentLength();
    if (contentLength < 0) {
      contentLength = ContentLengthUtils.DEFAULT;
    }
    return new ByteBufferMetadata(span, BoundedBuffersFactory.createStream(contentLength, charset));
  }

  public static CharBufferAndSpan createRequestCharBufferMetadata(
      HttpServletRequest httpServletRequest, Span span) {
    int contentLength = httpServletRequest.getContentLength();
    if (contentLength < 0) {
      contentLength = ContentLengthUtils.DEFAULT;
    }
    return new CharBufferAndSpan(span, BoundedBuffersFactory.createWriter(contentLength));
  }

  public static ByteBufferMetadata createResponseMetadata(
      HttpServletResponse httpServletResponse, Span span) {
    String contentType = httpServletResponse.getContentType();
    String charsetStr = ContentTypeUtils.parseCharset(contentType);
    Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);
    return new ByteBufferMetadata(span, BoundedBuffersFactory.createStream(charset));
  }

  public static CharBufferAndSpan createResponseCharBufferMetadata(
      HttpServletResponse httpServletResponse, Span span) {
    return new CharBufferAndSpan(span, BoundedBuffersFactory.createWriter());
  }
}
