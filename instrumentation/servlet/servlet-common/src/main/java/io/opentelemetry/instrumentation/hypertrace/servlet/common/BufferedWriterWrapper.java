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

package io.opentelemetry.instrumentation.hypertrace.servlet.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class BufferedWriterWrapper extends BufferedWriter {

  private final PrintWriter writer;
  private final CharBufferData charBufferData;

  public BufferedWriterWrapper(PrintWriter writer, CharBufferData charBufferData) {
    super(writer);
    this.writer = writer;
    this.charBufferData = charBufferData;
  }

  @Override
  public void write(char buf[]) throws IOException {
    write(buf, 0, buf.length);
  }

  @Override
  public void write(char buf[], int off, int len) throws IOException {
    writer.write(buf, off, len);
    charBufferData.appendData(buf, off, len);
  }

  @Override
  public void write(int c) throws IOException {
    writer.write(c);
    charBufferData.appendData(c);
  }

  @Override
  public void write(String s) throws IOException {
    write(s, 0, s.length());
  }

  @Override
  public void write(String s, int off, int len) throws IOException {
    writer.write(s, off, len);
    charBufferData.appendData(s, off, len);
  }
}
