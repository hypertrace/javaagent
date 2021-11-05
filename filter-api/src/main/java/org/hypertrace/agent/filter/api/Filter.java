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

package org.hypertrace.agent.filter.api;

import io.opentelemetry.api.trace.Span;
import java.util.Map;
import org.hypertrace.agent.filter.FilterRegistry;

/**
 * {@link Filter} evaluates given request/RPC and the result is used to block further processing of
 * the request. The instrumentations access filters via {@link FilterRegistry}.
 */
public interface Filter {

  /**
   * Evaluate the execution.
   *
   * @param headers are used for blocking evaluation.
   * @return filter result
   */
  boolean evaluateRequestHeaders(Span span, Map<String, String> headers);

  /**
   * Evaluate the execution.
   *
   * @param span of the HTTP request associated with this body
   * @param body request body
   * @param headers of the request associated with this body
   * @return filter result
   */
  boolean evaluateRequestBody(Span span, String body, Map<String, String> headers);
}
