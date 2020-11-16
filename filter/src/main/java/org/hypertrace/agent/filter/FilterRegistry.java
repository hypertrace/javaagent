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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Provides access to the {@link Filter} implementations. The {@link Filter} implementation are
 * found via Java service loader.
 */
public class FilterRegistry {

  private FilterRegistry() {}

  private static Filter filter;

  /**
   * Get {@link Filter}
   *
   * @return the filter evaluator.
   */
  public static Filter getFilter() {
    if (filter == null) {
      synchronized (FilterRegistry.class) {
        if (filter == null) {
          filter = load();
        }
      }
    }
    return filter;
  }

  private static Filter load() {
    ServiceLoader<Filter> filters = ServiceLoader.load(Filter.class);
    List<Filter> filterList = new ArrayList<>();
    filters.iterator().forEachRemaining(filterList::add);
    return new MultiFilter(filterList);
  }
}
