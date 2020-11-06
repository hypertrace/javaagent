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

package org.hypertrace.agent.blocking;

import java.util.Map;

/** Mock blocking evaluator, blocks execution if an attribute with "block" key is present. */
class MockBlockingEvaluator implements BlockingEvaluator {

  public static MockBlockingEvaluator INSTANCE = new MockBlockingEvaluator();

  private MockBlockingEvaluator() {}

  @Override
  public BlockingResult evaluate(Map<String, String> attributes) {
    if (attributes.containsKey("block")) {
      return new ExecutionBlocked("Header 'block' found");
    }
    return ExecutionNotBlocked.INSTANCE;
  }
}
