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
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentTypeCharsetUtils {
  private ContentTypeCharsetUtils() {}

  private static final Logger log = LoggerFactory.getLogger(ContentTypeCharsetUtils.class);

  // default for HTTP 1.1 https://www.w3.org/International/articles/http-charset/index
  private static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

  public static Charset toCharset(String charsetName) {
    if (charsetName == null || charsetName.isEmpty()) {
      return DEFAULT_CHARSET;
    }
    try {
      return Charset.forName(charsetName);
    } catch (Exception e) {
      log.error(
          "Could not parse encoding {} to charset, using default {}", charsetName, DEFAULT_CHARSET);
    }
    return DEFAULT_CHARSET;
  }
}
