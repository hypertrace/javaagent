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

package org.hypertrace.agent.core.bootstrap.instrumentation;

/**
 * Copy of the upstream {@link io.opentelemetry.javaagent.instrumentation.api.CallDepth} because we
 * need access to the package-private reset method.
 */
public class CallDepth {

  private int depth;

  CallDepth() {
    this.depth = 0;
  }

  public int getAndIncrement() {
    return this.depth++;
  }

  public int decrementAndGet() {
    return --this.depth;
  }

  void reset() {
    depth = 0;
  }
}
