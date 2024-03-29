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

package rw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class DelegatingBufferedReader extends BufferedReader {

  private final Reader delegate;

  public DelegatingBufferedReader(Reader delegate) {
    super(delegate);
    this.delegate = delegate;
  }

  @Override
  public int read(char[] cbuf) throws IOException {
    return delegate.read(cbuf);
  }
}
