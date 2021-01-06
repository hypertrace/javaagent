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

import java.nio.charset.Charset;

public class BoundedByteArrayOutputStreamFactory {

  public static final int DEFAULT_SIZE = 128;

  public static BoundedByteArrayOutputStream create(Charset charset) {
    return new BoundedByteArrayOutputStream(DEFAULT_SIZE, charset);
  }

  public static BoundedByteArrayOutputStream create(int initialSize, Charset charset) {
    if (initialSize > DEFAULT_SIZE) {
      initialSize = DEFAULT_SIZE;
    }
    return new BoundedByteArrayOutputStream(DEFAULT_SIZE, initialSize, charset);
  }
}
