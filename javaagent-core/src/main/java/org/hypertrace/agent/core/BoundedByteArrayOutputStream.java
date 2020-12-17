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

package org.hypertrace.agent.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * {@link ByteArrayOutputStream} with a bounded capacity. Write methods are no-op if the size
 * reaches the maximum capacity.
 */
public class BoundedByteArrayOutputStream extends ByteArrayOutputStream {

  private final int maxCapacity;

  BoundedByteArrayOutputStream(int maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

  BoundedByteArrayOutputStream(int maxCapacity, int size) {
    super(size);
    this.maxCapacity = maxCapacity;
  }

  @Override
  public synchronized void write(int b) {
    if (size() == maxCapacity) {
      return;
    }
    super.write(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    // ByteArrayOutputStream calls write(b, off, len)
    super.write(b);
  }

  @Override
  public synchronized void write(byte[] b, int off, int len) {
    int size = size();
    if (size + len > maxCapacity) {
      super.write(b, off, maxCapacity - size);
      return;
    }
    super.write(b, off, len);
  }
}
