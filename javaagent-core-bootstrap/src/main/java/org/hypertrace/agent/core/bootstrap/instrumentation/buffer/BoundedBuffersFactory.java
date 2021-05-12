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

package org.hypertrace.agent.core.bootstrap.instrumentation.buffer;

import java.nio.charset.Charset;
import org.hypertrace.agent.core.bootstrap.config.HypertraceConfig;

public class BoundedBuffersFactory {

  public static final int MAX_SIZE =
      HypertraceConfig.get().getDataCapture().getBodyMaxSizeBytes().getValue();

  public static BoundedByteArrayOutputStream createStream(Charset charset) {
    return new BoundedByteArrayOutputStream(MAX_SIZE, charset);
  }

  public static BoundedByteArrayOutputStream createStream(int initialSize, Charset charset) {
    if (initialSize > MAX_SIZE) {
      initialSize = MAX_SIZE;
    }
    return new BoundedByteArrayOutputStream(MAX_SIZE, initialSize, charset);
  }

  public static BoundedCharArrayWriter createWriter() {
    return new BoundedCharArrayWriter(MAX_SIZE);
  }

  public static BoundedCharArrayWriter createWriter(int initialSize) {
    if (initialSize > MAX_SIZE) {
      initialSize = MAX_SIZE;
    }
    return new BoundedCharArrayWriter(MAX_SIZE, initialSize);
  }
}
