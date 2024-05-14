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

package org;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import java.io.IOException;

public class DelegatingServletOutputStream extends ServletOutputStream {

  private final ServletOutputStream delegate;

  public DelegatingServletOutputStream(ServletOutputStream delegate) {
    this.delegate = delegate;
  }

  @Override
  public void write(int b) throws IOException {
    this.delegate.write(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    this.delegate.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    this.delegate.write(b, off, len);
  }

  @Override
  public void flush() throws IOException {
    this.delegate.flush();
  }

  @Override
  public void close() throws IOException {
    this.delegate.close();
  }

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public void setWriteListener(WriteListener writeListener) {}
}
