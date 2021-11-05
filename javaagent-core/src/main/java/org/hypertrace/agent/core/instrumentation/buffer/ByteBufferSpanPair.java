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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.hypertrace.agent.core.TriFunction;
import org.hypertrace.agent.core.instrumentation.HypertraceEvaluationException;

public class ByteBufferSpanPair {

  public final Span span;
  public final BoundedByteArrayOutputStream buffer;
  private boolean bufferCaptured;
  private final TriFunction<Span, String, Map<String, String>, Boolean> filter;

  public ByteBufferSpanPair(
      Span span,
      BoundedByteArrayOutputStream buffer,
      TriFunction<Span, String, Map<String, String>, Boolean> filter) {
    this.span = span;
    this.buffer = buffer;
    this.filter = Objects.requireNonNull(filter);
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
    final boolean block;
    block = filter.apply(span, requestBody, Collections.emptyMap());
    if (block) {
      throw new HypertraceEvaluationException();
    }
  }
}
