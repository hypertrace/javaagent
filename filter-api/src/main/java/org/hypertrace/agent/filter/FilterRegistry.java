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
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import org.hypertrace.agent.core.EnvironmentConfig;
import org.hypertrace.agent.filter.api.Filter;
import org.hypertrace.agent.filter.spi.FilterProvider;

/**
 * Provides access to the {@link Filter} implementations. The {@link Filter} implementation are
 * created via Java service loader.
 *
 * @see Filter
 * @see FilterProvider
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
    ServiceLoader<FilterProvider> providers = ServiceLoader.load(FilterProvider.class);
    List<Filter> filters = new ArrayList<>();
    Iterator<FilterProvider> iterator = providers.iterator();
    while (iterator.hasNext()) {
      FilterProvider provider = iterator.next();
      String disabled =
          EnvironmentConfig.getProperty(getProviderDisabledPropertyName(provider.getClass()));
      if ("true".equalsIgnoreCase(disabled)) {
        continue;
      }
      Filter filter = provider.create();
      filters.add(filter);
    }
    return new MultiFilter(filters);
  }

  public static String getProviderDisabledPropertyName(Class<?> clazz) {
    return String.format("ht.filter.provider.%s.disabled", clazz.getSimpleName());
  }
}
