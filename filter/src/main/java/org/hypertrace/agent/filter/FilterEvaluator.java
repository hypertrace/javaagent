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

package org.hypertrace.agent.filter;

import io.opentelemetry.trace.Span;
import java.util.Map;

/**
 * {@link FilterEvaluator} evaluates given request/RPC and the result is used to block further
 * processing of the request.
 */
public interface FilterEvaluator {

  /**
   * Evaluate the execution.
   *
   * @param headers are used for blocking evaluation.
   * @return filter result
   */
  FilterResult evaluateRequestHeaders(Span span, Map<String, String> headers);

  /**
   * Evaluate the execution.
   *
   * @param body request body
   * @return filter result
   */
  FilterResult evaluateRequestBody(Span span, String body);
}
