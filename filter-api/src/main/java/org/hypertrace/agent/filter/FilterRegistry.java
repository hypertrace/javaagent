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

import com.google.protobuf.StringValue;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.hypertrace.agent.core.config.EnvironmentConfig;
import org.hypertrace.agent.core.config.HypertraceConfig;
import org.hypertrace.agent.filter.api.Filter;
import org.hypertrace.agent.filter.spi.FilterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to the {@link Filter} implementations. The {@link Filter} implementation are
 * created via Java service loader.
 *
 * @see Filter
 * @see FilterProvider
 */
public class FilterRegistry {

  private static final Logger logger = LoggerFactory.getLogger(FilterRegistry.class);

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
    ClassLoader cl = loadJars();
    ServiceLoader<FilterProvider> providers = ServiceLoader.load(FilterProvider.class, cl);
    List<Filter> filters = new ArrayList<>();
    for (FilterProvider provider : providers) {
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

  private static ClassLoader loadJars() {
    List<StringValue> jarPaths = HypertraceConfig.get().getJavaagent().getFilterJarPathsList();
    URL[] urls = new URL[jarPaths.size()];
    int i = 0;
    for (StringValue jarPath : jarPaths) {
      try {
        URL url = new URL("file", "", -1, jarPath.getValue());
        urls[i] = url;
        i++;
      } catch (MalformedURLException e) {
        logger.warn(
            String.format("Malformed URL exception for jar on path: %s", jarPath.getValue()), e);
      }
    }
    return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
  }

  public static String getProviderDisabledPropertyName(Class<?> clazz) {
    return String.format("ht.filter.provider.%s.disabled", clazz.getSimpleName());
  }
}
