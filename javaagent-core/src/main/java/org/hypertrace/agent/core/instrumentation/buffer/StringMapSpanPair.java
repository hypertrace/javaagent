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

package org.hypertrace.agent.core.instrumentation.buffer;

import static org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils.convertToJSONString;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.util.Map;

/** Created to represent the request body that is in x-www-form-urlencoded format. */
public class StringMapSpanPair {

  public final Span span;
  public final Map<String, String> headers;
  public final Map<String, String> stringMap;

  /** A flag to signalize that map has been added to span. */
  private boolean mapCaptured;

  public StringMapSpanPair(Span span, Map<String, String> stringMap, Map<String, String> headers) {
    this.span = span;
    this.stringMap = stringMap;
    this.headers = headers;
  }

  /**
   * Capture the contents of the x-www-form-urlencoded body in the span with provided attributeKey.
   *
   * @param attributeKey the attributeKey to store contents of the captured body in the span
   */
  public void captureBody(AttributeKey<String> attributeKey) {
    if (!mapCaptured) {
      String json = convertToJSONString(stringMap);
      span.setAttribute(attributeKey, json);
      mapCaptured = true;
    }
  }
}
