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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public class DelegatingPrintWriter extends PrintWriter {

  private final Writer delegate;

  public DelegatingPrintWriter(Writer delegate) {
    super(delegate);
    this.delegate = delegate;
  }

  @Override
  public void write(char[] buf) {
    try {
      this.delegate.write(buf);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
