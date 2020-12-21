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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hypertrace.agent.core.GlobalObjectRegistry;
import org.hypertrace.agent.core.GlobalObjectRegistry.SpanAndBuffer;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InputStreamInstrumentationModuleTest extends AbstractInstrumenterTest {

  private static final AttributeKey<String> ATTRIBUTE_KEY =
      AttributeKey.stringKey("captured-stream");
  private static final String STR = "hello";

  @Test
  public void read() {
    InputStream inputStream = new TestByteArrayInputStream(STR.getBytes());
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
    InputStream inputStream = new TestByteArrayInputStream(STR.getBytes());
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
    InputStream inputStream = new TestByteArrayInputStream(STR.getBytes());
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

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    GlobalObjectRegistry.inputStreamToSpanAndBufferMap.put(
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

  /**
   * Each method has to be overridden because OTEL agent ignores classes from java.
   * https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/8baa897e8bf09359da848aaaa98d2b4eb7fbf4c1/javaagent-tooling/src/main/java/io/opentelemetry/javaagent/tooling/matcher/GlobalIgnoresMatcher.java#L105
   */
  static class TestByteArrayInputStream extends ByteArrayInputStream {

    public TestByteArrayInputStream(byte[] buf) {
      super(buf);
    }

    @Override
    public synchronized int read() {
      return super.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
      return super.read(b);
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) {
      return super.read(b, off, len);
    }
  }
}
