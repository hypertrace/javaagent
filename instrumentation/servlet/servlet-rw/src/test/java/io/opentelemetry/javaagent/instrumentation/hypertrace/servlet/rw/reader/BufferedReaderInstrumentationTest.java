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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.rw.reader;

import io.opentelemetry.api.trace.Span;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import org.BufferedReaderPrintWriterContextAccess;
import org.TestBufferedReader;
import org.hypertrace.agent.core.instrumentation.buffer.*;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BufferedReaderInstrumentationTest extends AbstractInstrumenterTest {

  private static final String TEST_SPAN_NAME = "foo";
  private static final String BODY = "boobar";

  @Test
  public void read() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    BufferedReader bufferedReader = new TestBufferedReader(new CharArrayReader(BODY.toCharArray()));

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    CharBufferSpanPair bufferSpanPair = new CharBufferSpanPair(span, buffer);

    BufferedReaderPrintWriterContextAccess.addToBufferedReaderContext(bufferedReader, bufferSpanPair);

    while (bufferedReader.read() != -1) {}
    Assertions.assertEquals(BODY, buffer.toString());
  }

  @Test
  public void read_callDepth_isCleared() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    BufferedReader bufferedReader = new TestBufferedReader(new CharArrayReader(BODY.toCharArray()));
    bufferedReader.read();

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    CharBufferSpanPair bufferSpanPair = new CharBufferSpanPair(span, buffer);

    BufferedReaderPrintWriterContextAccess.addToBufferedReaderContext(bufferedReader, bufferSpanPair);

    while (bufferedReader.read() != -1) {}
    Assertions.assertEquals(BODY.substring(1), buffer.toString());
  }

  @Test
  public void read_char_arr() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    BufferedReader bufferedReader = new TestBufferedReader(new CharArrayReader(BODY.toCharArray()));

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    CharBufferSpanPair bufferSpanPair = new CharBufferSpanPair(span, buffer);

    BufferedReaderPrintWriterContextAccess.addToBufferedReaderContext(bufferedReader, bufferSpanPair);

    while (bufferedReader.read(new char[BODY.length()]) != -1) {}
    Assertions.assertEquals(BODY, buffer.toString());
  }

  @Test
  public void read_callDepth_char_arr() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    BufferedReader bufferedReader = new TestBufferedReader(new CharArrayReader(BODY.toCharArray()));
    bufferedReader.read(new char[2]);

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    CharBufferSpanPair bufferSpanPair = new CharBufferSpanPair(span, buffer);

    BufferedReaderPrintWriterContextAccess.addToBufferedReaderContext(bufferedReader, bufferSpanPair);

    while (bufferedReader.read(new char[BODY.length()]) != -1) {}
    Assertions.assertEquals(BODY.substring(2), buffer.toString());
  }

  @Test
  public void read_char_arr_offset() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    BufferedReader bufferedReader = new TestBufferedReader(new CharArrayReader(BODY.toCharArray()));

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    CharBufferSpanPair bufferSpanPair = new CharBufferSpanPair(span, buffer);

    BufferedReaderPrintWriterContextAccess.addToBufferedReaderContext(bufferedReader, bufferSpanPair);

    bufferedReader.read(new char[BODY.length()], 0, 2);
    bufferedReader.read(new char[BODY.length()], 2, BODY.length()-2);
    Assertions.assertEquals(BODY, buffer.toString());
  }

  @Test
  public void readLine() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    BufferedReader bufferedReader = new TestBufferedReader(new CharArrayReader((BODY+"\n").toCharArray()));

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    CharBufferSpanPair bufferSpanPair = new CharBufferSpanPair(span, buffer);

    BufferedReaderPrintWriterContextAccess.addToBufferedReaderContext(bufferedReader, bufferSpanPair);

    bufferedReader.readLine();
    Assertions.assertEquals(BODY, buffer.toString());
  }
}
