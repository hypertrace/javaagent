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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.ContextAccessor;
import org.hypertrace.agent.core.instrumentation.SpanAndBuffer;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InputStreamInstrumentationModuleTest extends AbstractInstrumenterTest {

  private static final AttributeKey<String> ATTRIBUTE_KEY =
      AttributeKey.stringKey("captured-stream");
  private static final String STR = "hello";

  @Test
  public void read() {
    InputStream inputStream = new ByteArrayInputStream(STR.getBytes());
    read(
        inputStream,
        () -> {
          while (true) {
            try {
              if (!(inputStream.read() != -1)) break;
            } catch (IOException e) {
              e.printStackTrace();
            }
            ;
          }
        },
        STR);
  }

  @Test
  public void readBytes() {
    InputStream inputStream = new ByteArrayInputStream(STR.getBytes());
    read(
        inputStream,
        () -> {
          while (true) {
            try {
              if (!(inputStream.read(new byte[10]) != -1)) break;
            } catch (IOException e) {
              e.printStackTrace();
            }
            ;
          }
        },
        STR);
  }

  @Test
  public void readBytesOffset() {
    InputStream inputStream = new ByteArrayInputStream(STR.getBytes());
    read(
        inputStream,
        () -> {
          while (true) {
            try {
              if (!(inputStream.read(new byte[10], 0, 10) != -1)) break;
            } catch (IOException e) {
              e.printStackTrace();
            }
            ;
          }
        },
        STR);
  }

  private void read(InputStream inputStream, Runnable read, String expected) {
    Span span = TEST_TRACER.spanBuilder("test-span").startSpan();

    BoundedByteArrayOutputStream buffer =
        BoundedBuffersFactory.createStream(StandardCharsets.ISO_8859_1);
    ContextAccessor.addToInputStreamContext(
        inputStream, new SpanAndBuffer(span, buffer, ATTRIBUTE_KEY, StandardCharsets.ISO_8859_1));

    read.run();
    span.end();

    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());

    List<SpanData> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    SpanData spanData = trace.get(0);
    Assertions.assertEquals(expected, spanData.getAttributes().get(ATTRIBUTE_KEY));
  }
}
