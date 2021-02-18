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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.rw.writer;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import org.BufferedReaderPrintWriterContextAccess;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PrintWriterInstrumentationTest extends AbstractInstrumenterTest {

  private static final String BODY = "boobar";

  @Test
  public void write_single_char() {
    PrintWriter printWriter = new PrintWriter(new CharArrayWriter());

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    BufferedReaderPrintWriterContextAccess.addToPrintWriterContext(printWriter, buffer);

    char[] chars = BODY.toCharArray();
    for (int i = 0; i < BODY.length(); i++) {
      printWriter.write(chars[i]);
    }
    Assertions.assertEquals(BODY, buffer.toString());
  }

  @Test
  public void write_char_arr() {
    PrintWriter printWriter = new PrintWriter(new CharArrayWriter());

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    BufferedReaderPrintWriterContextAccess.addToPrintWriterContext(printWriter, buffer);

    printWriter.write(BODY.toCharArray());
    Assertions.assertEquals(BODY, buffer.toString());
  }

  @Test
  public void write_char_arr_offset() {
    PrintWriter printWriter = new PrintWriter(new CharArrayWriter());

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    BufferedReaderPrintWriterContextAccess.addToPrintWriterContext(printWriter, buffer);

    char[] chars = BODY.toCharArray();
    printWriter.write(chars, 0, 2);
    printWriter.write(chars, 2, chars.length - 2);
    Assertions.assertEquals(BODY, buffer.toString());
  }

  @Test
  public void write_str() {
    PrintWriter printWriter = new PrintWriter(new CharArrayWriter());

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    BufferedReaderPrintWriterContextAccess.addToPrintWriterContext(printWriter, buffer);

    printWriter.write(BODY);
    Assertions.assertEquals(BODY, buffer.toString());
  }

  @Test
  public void write_str_offset() {
    PrintWriter printWriter = new PrintWriter(new CharArrayWriter());

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    BufferedReaderPrintWriterContextAccess.addToPrintWriterContext(printWriter, buffer);

    printWriter.write(BODY, 0, 2);
    printWriter.write(BODY, 2, BODY.length() - 2);
    Assertions.assertEquals(BODY, buffer.toString());
  }

  @Test
  public void print_str() {
    PrintWriter printWriter = new PrintWriter(new CharArrayWriter());

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    BufferedReaderPrintWriterContextAccess.addToPrintWriterContext(printWriter, buffer);

    printWriter.print(BODY);
    Assertions.assertEquals(BODY, buffer.toString());
  }

  @Test
  public void println() {
    PrintWriter printWriter = new PrintWriter(new CharArrayWriter());

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    BufferedReaderPrintWriterContextAccess.addToPrintWriterContext(printWriter, buffer);

    printWriter.println();
    Assertions.assertEquals("\n", buffer.toString());
  }

  @Test
  public void println_str() {
    PrintWriter printWriter = new PrintWriter(new CharArrayWriter());

    BoundedCharArrayWriter buffer = BoundedBuffersFactory.createWriter();
    BufferedReaderPrintWriterContextAccess.addToPrintWriterContext(printWriter, buffer);

    printWriter.println(BODY);
    Assertions.assertEquals(BODY + "\n", buffer.toString());
  }
}
