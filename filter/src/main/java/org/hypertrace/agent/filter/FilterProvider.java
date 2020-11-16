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

/**
 * {@link FilterProvider} creates {@link Filter}.
 *
 * <p>The implementation is discovered via Java service loader API - each implementation has to be
 * registered in {@code META-INF/services/}.
 */
public interface FilterProvider {

  /**
   * Create filter instance.
   *
   * @return a filter instance.
   */
  Filter create();
}
