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
import java.io.UnsupportedEncodingException;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;

public class ByteBufferSpanPair {

  public final Span span;
  public final BoundedByteArrayOutputStream buffer;

  public ByteBufferSpanPair(Span span, BoundedByteArrayOutputStream buffer) {
    this.span = span;
    this.buffer = buffer;
  }

  public void captureBody() {
    String requestBody = null;
    try {
      requestBody = buffer.toStringWithSuppliedCharset();
    } catch (UnsupportedEncodingException e) {
      // ignore charset has been parsed before
    }
    span.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, requestBody);
  }
}
