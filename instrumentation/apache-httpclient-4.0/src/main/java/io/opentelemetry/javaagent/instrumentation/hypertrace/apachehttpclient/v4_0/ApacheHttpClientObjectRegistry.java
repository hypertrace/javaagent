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

package io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpclient.v4_0;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.cache.Cache;
import org.apache.http.HttpEntity;

public class ApacheHttpClientObjectRegistry {

  public static final Cache<HttpEntity, SpanAndAttributeKey> entityToSpan = Cache.weak();

  public static class SpanAndAttributeKey {
    public final Span span;
    public final AttributeKey<String> attributeKey;

    public SpanAndAttributeKey(Span span, AttributeKey<String> attributeKey) {
      this.span = span;
      this.attributeKey = attributeKey;
    }
  }
}
