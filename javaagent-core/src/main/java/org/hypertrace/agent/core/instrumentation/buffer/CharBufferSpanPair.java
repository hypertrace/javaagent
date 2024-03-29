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
import java.io.IOException;
import java.util.Map;
import org.hypertrace.agent.core.TriFunction;
import org.hypertrace.agent.core.filter.FilterResult;
import org.hypertrace.agent.core.instrumentation.HypertraceEvaluationException;

public class CharBufferSpanPair {

  public final Span span;
  public final Map<String, String> headers;
  private final BoundedCharArrayWriter buffer;
  private final TriFunction<Span, String, Map<String, String>, FilterResult> filter;

  /**
   * A flag to signalize that buffer has been added to span. For instance Jetty calls reader#read in
   * recycle method and this flag prevents capturing the payload twice.
   */
  private boolean bufferCaptured;

  public CharBufferSpanPair(
      Span span,
      BoundedCharArrayWriter buffer,
      TriFunction<Span, String, Map<String, String>, FilterResult> filter,
      Map<String, String> headers) {
    this.span = span;
    this.buffer = buffer;
    this.headers = headers;
    this.filter = filter;
  }

  public void captureBody(AttributeKey<String> attributeKey) {
    if (bufferCaptured) {
      return;
    }
    bufferCaptured = true;
    String requestBody = buffer.toString();
    span.setAttribute(attributeKey, requestBody);
    final FilterResult filterResult;
    filterResult = filter.apply(span, requestBody, headers);
    if (filterResult.shouldBlock()) {
      throw new HypertraceEvaluationException(filterResult);
    }
  }

  public void writeToBuffer(byte singleByte) {
    bufferCaptured = false;
    buffer.write(singleByte);
  }

  public void writeToBuffer(char[] c, int offset, int len) {
    bufferCaptured = false;
    buffer.write(c, offset, len);
  }

  public void writeToBuffer(char[] c) throws IOException {
    bufferCaptured = false;
    buffer.write(c);
  }

  public void writeLine(String line) throws IOException {
    bufferCaptured = false;
    buffer.write(line);
  }
}
