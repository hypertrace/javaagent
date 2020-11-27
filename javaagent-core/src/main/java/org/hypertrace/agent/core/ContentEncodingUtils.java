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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentEncodingUtils {
  private ContentEncodingUtils() {}

  private static final Logger log = LoggerFactory.getLogger(ContentEncodingUtils.class);

  // default for HTTP 1.1 https://www.w3.org/International/articles/http-charset/index
  private static final Charset DEFAULT = Charset.forName("ISO-8859-1");

  public static Charset toCharset(String encoding) {
    if (encoding == null || encoding.isEmpty()) {
      return DEFAULT;
    }
    try {
      return Charset.forName(encoding);
    } catch (Exception e) {
      log.error("Could not parse encoding {} to charset, using default {}", encoding, DEFAULT);
    }
    return DEFAULT;
  }
}
