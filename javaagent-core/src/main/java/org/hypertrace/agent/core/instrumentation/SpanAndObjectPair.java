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

package org.hypertrace.agent.core.instrumentation;

import io.opentelemetry.api.trace.Span;
import java.util.Map;

public class SpanAndObjectPair {

  private final Span span;
  private final Map<String, String> headers;
  private Object associatedObject;

  public SpanAndObjectPair(Span span, Map<String, String> headers) {
    this.span = span;
    this.headers = headers;
  }

  public Span getSpan() {
    return span;
  }

  public Object getAssociatedObject() {
    return associatedObject;
  }

  public void setAssociatedObject(Object associatedObject) {
    this.associatedObject = associatedObject;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }
}
