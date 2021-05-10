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

package io.opentelemetry.javaagent.instrumentation.hypertrace.java.inputstream;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.SpanAndBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputStreamUtils {

  private InputStreamUtils() {}

  public static final Logger log = LoggerFactory.getLogger(InputStreamUtils.class);

  private static final Tracer TRACER =
      GlobalOpenTelemetry.get().getTracer("org.hypertrace.java.inputstream");

  /**
   * Adds an attribute to span. If the span is ended it adds the attributed to a newly created
   * child.
   */
  public static void addAttribute(Span span, AttributeKey<String> attributeKey, String value) {
    if (span.isRecording()) {
      span.setAttribute(attributeKey, value);
    } else {
      TRACER
          .spanBuilder(HypertraceSemanticAttributes.ADDITIONAL_DATA_SPAN_NAME)
          .setParent(Context.root().with(span))
          .setAttribute(attributeKey, value)
          .startSpan()
          .end();
    }
  }

  public static void addBody(
      Span span, AttributeKey<String> attributeKey, ByteArrayOutputStream buffer, Charset charset) {
    try {
      String body = buffer.toString(charset.name());
      InputStreamUtils.addAttribute(span, attributeKey, body);
    } catch (UnsupportedEncodingException e) {
      log.error("Failed to parse encofing from charset {}", charset, e);
    }
  }

  public static SpanAndBuffer check(
      InputStream inputStream, ContextStore<InputStream, SpanAndBuffer> contextStore) {
    SpanAndBuffer spanAndBuffer = contextStore.get(inputStream);
    if (spanAndBuffer == null) {
      return null;
    }

    CallDepthThreadLocalMap.incrementCallDepth(InputStream.class);
    return spanAndBuffer;
  }

  public static void read(
      InputStream inputStream,
      SpanAndBuffer spanAndBuffer,
      ContextStore<InputStream, SpanAndBuffer> contextStore,
      int read) {
    if (read != -1) {
      spanAndBuffer.byteArrayBuffer.write((byte) read);
    } else if (read == -1) {
      InputStreamUtils.addBody(
          spanAndBuffer.span,
          spanAndBuffer.attributeKey,
          spanAndBuffer.byteArrayBuffer,
          spanAndBuffer.charset);
      contextStore.put(inputStream, null);
    }
  }

  public static void read(
      InputStream inputStream, SpanAndBuffer spanAndBuffer, int read, byte[] b) {
    if (read > 0) {
      spanAndBuffer.byteArrayBuffer.write(b, 0, read);
    } else if (read == -1) {
      InputStreamUtils.addBody(
          spanAndBuffer.span,
          spanAndBuffer.attributeKey,
          spanAndBuffer.byteArrayBuffer,
          spanAndBuffer.charset);
      InstrumentationContext.get(InputStream.class, SpanAndBuffer.class).put(inputStream, null);
    }
  }

  public static void read(
      InputStream inputStream,
      SpanAndBuffer spanAndBuffer,
      ContextStore<InputStream, SpanAndBuffer> contextStore,
      int read,
      byte[] b,
      int off,
      int len) {
    if (read > 0) {
      spanAndBuffer.byteArrayBuffer.write(b, off, read);
    } else if (read == -1) {
      InputStreamUtils.addBody(
          spanAndBuffer.span,
          spanAndBuffer.attributeKey,
          spanAndBuffer.byteArrayBuffer,
          spanAndBuffer.charset);
      contextStore.put(inputStream, null);
    }
  }

  public static void readAll(
      InputStream inputStream,
      SpanAndBuffer spanAndBuffer,
      ContextStore<InputStream, SpanAndBuffer> contextStore,
      byte[] b)
      throws IOException {
    spanAndBuffer.byteArrayBuffer.write(b);
    contextStore.put(inputStream, null);
  }

  public static void readNBytes(
      InputStream inputStream,
      SpanAndBuffer spanAndBuffer,
      ContextStore<InputStream, SpanAndBuffer> contextStore,
      int read,
      byte[] b,
      int off,
      int len) {
    if (read == 0) {
      InputStreamUtils.addBody(
          spanAndBuffer.span,
          spanAndBuffer.attributeKey,
          spanAndBuffer.byteArrayBuffer,
          spanAndBuffer.charset);
      contextStore.put(inputStream, null);
    } else {
      spanAndBuffer.byteArrayBuffer.write(b, off, read);
    }
  }
}
