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

package org.hypertrace.agent.otel.extensions.sampler;

class GlobToRegex {
  private GlobToRegex() {}

  public static String convertGlobToRegex(String pattern) {
    StringBuilder sb = new StringBuilder(pattern.length());
    for (int i = 0; i < pattern.length(); i++) {
      char ch = pattern.charAt(i);
      if (ch == '*') {
        if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') {
          sb.append(".*");
        } else {
          sb.append("[^\\/]*(|\\/)+");
        }
      } else if (ch == '*' && i > 0 && pattern.charAt(i - 1) == '*') {
        continue;
      } else {
        sb.append(ch);
      }
    }
    return sb.toString();
  }
}
