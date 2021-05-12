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

package org.hypertrace.agent.core.bootstrap.instrumentation.utils;

import org.hypertrace.agent.core.bootstrap.instrumentation.buffer.BoundedBuffersFactory;

public class ContentLengthUtils {
  private ContentLengthUtils() {}

  // default content length
  public static final int DEFAULT = BoundedBuffersFactory.MAX_SIZE;

  public static int parseLength(CharSequence lengthStr) {
    if (lengthStr == null || lengthStr.length() == 0) {
      return DEFAULT;
    }

    try {
      return Integer.parseInt(lengthStr.toString());
    } catch (Exception ex) {
      return DEFAULT;
    }
  }
}
