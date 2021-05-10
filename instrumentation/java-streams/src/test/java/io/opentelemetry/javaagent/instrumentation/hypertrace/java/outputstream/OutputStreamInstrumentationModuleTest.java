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

package io.opentelemetry.javaagent.instrumentation.hypertrace.java.outputstream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.ContextAccessor;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OutputStreamInstrumentationModuleTest extends AbstractInstrumenterTest {
  private static final String STR1 = "hellofoobarrandom";
  private static final String STR2 = "someother";

  private static final int MAX_SIZE = Integer.MAX_VALUE;
  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  @Test
  public void write() {
    OutputStream outputStream = new ByteArrayOutputStream();
    write(
        outputStream,
        () -> {
          for (byte ch : STR1.getBytes()) {
            try {
              outputStream.write(ch);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        },
        STR1);
  }

  @Test
  public void writeBytes() {
    OutputStream outputStream = new ByteArrayOutputStream();
    write(
        outputStream,
        () -> {
          try {
            outputStream.write(STR1.getBytes());
            outputStream.write(STR2.getBytes());
          } catch (IOException e) {
            e.printStackTrace();
          }
        },
        STR1 + STR2);
  }

  @Test
  public void writeBytesOffset() {
    OutputStream outputStream = new ByteArrayOutputStream();
    write(
        outputStream,
        () -> {
          try {
            outputStream.write(STR1.getBytes(), 0, STR1.length() / 2);
            outputStream.write(STR1.getBytes(), STR1.length() / 2, STR1.length() / 2 + 1);
            outputStream.write(STR2.getBytes());
          } catch (IOException e) {
            e.printStackTrace();
          }
        },
        STR1 + STR2);
  }

  private void write(OutputStream outputStream, Runnable read, String expected) {
    BoundedByteArrayOutputStream buffer =
        BoundedBuffersFactory.createStream(MAX_SIZE, DEFAULT_CHARSET);
    ContextAccessor.addToOutputStreamContext(outputStream, buffer);
    read.run();
    Assertions.assertEquals(expected, buffer.toString());
  }
}
