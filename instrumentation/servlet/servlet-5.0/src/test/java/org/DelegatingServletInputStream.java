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

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.IOException;

public class DelegatingServletInputStream extends ServletInputStream {

  private final ServletInputStream wrapped;

  public DelegatingServletInputStream(ServletInputStream wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public int read() throws IOException {
    return wrapped.read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return wrapped.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return wrapped.read(b, off, len);
  }

  @Override
  public int readLine(byte[] b, int off, int len) throws IOException {
    return wrapped.readLine(b, off, len);
  }

  @Override
  public boolean isFinished() {
    return false;
  }

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public void setReadListener(ReadListener readListener) {}
}
