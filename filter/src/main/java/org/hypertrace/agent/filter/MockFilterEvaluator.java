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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mock blocking evaluator, blocks execution if an attribute with "block" key is present. */
class MockFilterEvaluator implements FilterEvaluator {

  private MockFilterEvaluator() {}

  public static MockFilterEvaluator INSTANCE = new MockFilterEvaluator();

  private static ExecutionBlocked EXECUTION_BLOCKED = executionBlocked();

  @Override
  public FilterResult evaluateRequestHeaders(Map<String, String> headers) {
    if (headers.containsKey("block")) {
      return EXECUTION_BLOCKED;
    }
    return ExecutionNotBlocked.INSTANCE;
  }

  @Override
  public FilterResult evaluateRequestBody(String body) {
    return ExecutionNotBlocked.INSTANCE;
  }

  private static ExecutionBlocked executionBlocked() {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("hypertrace.mock.filter.result", "true");
    return new ExecutionBlocked(Collections.unmodifiableMap(attributes));
  }
}
