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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;

public class BoundedCharArrayWriter extends CharArrayWriter {

  private final int maxCapacity;

  BoundedCharArrayWriter(int maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

  BoundedCharArrayWriter(int maxCapacity, int initialSize) {
    super(initialSize);
    this.maxCapacity = maxCapacity;
  }

  @Override
  public void write(int c) {
    if (size() == maxCapacity) {
      return;
    }
    super.write(c);
  }

  @Override
  public void write(char[] c, int off, int len) {
    int size = size();
    if (size + len > maxCapacity) {
      super.write(c, off, maxCapacity - size);
      return;
    }
    super.write(c, off, len);
  }

  @Override
  public void write(String str, int off, int len) {
    int size = size();
    if (size + len > maxCapacity) {
      super.write(str, off, maxCapacity - size);
      return;
    }
    super.write(str, off, len);
  }

  @Override
  public void writeTo(Writer out) throws IOException {
    // don't need to override, it will copy what is buffered
    super.writeTo(out);
  }

  @Override
  public CharArrayWriter append(CharSequence csq) {
    // the same impl as the super class
    String s = (csq == null ? "null" : csq.toString());
    this.write(s, 0, s.length());
    return this;
  }

  @Override
  public CharArrayWriter append(CharSequence csq, int start, int end) {
    String s = (csq == null ? "null" : csq).subSequence(start, end).toString();
    write(s, 0, s.length());
    return this;
  }

  @Override
  public CharArrayWriter append(char c) {
    write(c);
    return this;
  }

  @Override
  public void write(char[] cbuf) throws IOException {
    this.write(cbuf, 0, cbuf.length);
  }

  @Override
  public void write(String str) throws IOException {
    this.write(str, 0, str.length());
  }
}
