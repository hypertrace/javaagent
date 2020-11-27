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

package io.opentelemetry.instrumentation.hypertrace.apache.httpclient;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamUtils {

  private InputStreamUtils() {}

  private static final Tracer TRACER =
      OpenTelemetry.getGlobalTracer("org.hypertrace.java.inputstream");

  /**
   * Adds an attribute to span. If the span is ended it adds the attributed to a newly created
   * child.
   */
  public static void addAttribute(Span span, AttributeKey<String> attributeKey, String value) {
    System.out.printf("Captured %s body is %s\n", attributeKey.getKey(), value);
    if (span.isRecording()) {
      span.setAttribute(attributeKey, value);
    } else {
      System.out.println("parent is");
      System.out.println(span);
      TRACER
          .spanBuilder("additional-data")
          .setParent(Context.root().with(span))
          .setAttribute(attributeKey, value)
          .startSpan()
          .end();
    }
  }

  public static byte[] readToArr(InputStream inputStream, int contentSize) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream(contentSize);
    byte ch;
    while ((ch = (byte) inputStream.read()) != -1) {
      buffer.write(ch);
    }
    return buffer.toByteArray();
  }
}
