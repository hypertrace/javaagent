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

package org.hypertrace.agent.core.instrumentation.buffer;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.io.UnsupportedEncodingException;

public class ByteBufferSpanPair {

  public final Span span;
  public final BoundedByteArrayOutputStream buffer;
  private boolean bufferCaptured;

  public ByteBufferSpanPair(Span span, BoundedByteArrayOutputStream buffer) {
    this.span = span;
    this.buffer = buffer;
  }

  public void captureBody(AttributeKey<String> attributeKey) {
    if (bufferCaptured) {
      return;
    }
    bufferCaptured = true;

    String requestBody = null;
    try {
      requestBody = buffer.toStringWithSuppliedCharset();
    } catch (UnsupportedEncodingException e) {
      // ignore charset has been parsed before
    }
    span.setAttribute(attributeKey, requestBody);
  }
}
