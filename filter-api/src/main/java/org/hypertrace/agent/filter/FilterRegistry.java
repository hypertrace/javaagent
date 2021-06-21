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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
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
          try {
            filter = load(Collections.emptyList());
          } catch (Throwable t) {
            logger.error("Throwable thrown while loading filter jars", t);
          }
        }
      }
    }
    return filter;
  }

  /**
   * Initializes the registry by loading the filters. This method should be called only once at
   * javaagent startup.
   *
   * @param jarPaths paths to filter jar files.
   */
  public static void initialize(List<String> jarPaths) {
    try {
      filter = load(jarPaths);
    } catch (Throwable t) {
      logger.error("Throwable thrown while loading filter jars", t);
    }
  }

  private static Filter load(List<String> jarPaths) {
    ClassLoader cl = loadJars(jarPaths);
    ServiceLoader<FilterProvider> providers = ServiceLoader.load(FilterProvider.class, cl);
    List<Filter> filters = new ArrayList<>();
    for (FilterProvider provider : providers) {
      String disabled = getProperty(getProviderDisabledPropertyName(provider.getClass()));
      if ("true".equalsIgnoreCase(disabled)) {
        continue;
      }
      Filter filter = provider.create();
      filters.add(filter);
    }
    return new MultiFilter(filters);
  }

  private static ClassLoader loadJars(List<String> jarPaths) {
    URL[] urls = new URL[jarPaths.size()];
    int i = 0;
    for (String jarPath : jarPaths) {
      try {
        URL url = new URL("file", "", -1, jarPath);
        urls[i] = url;
        i++;
      } catch (MalformedURLException e) {
        logger.warn(String.format("Malformed URL exception for jar on path: %s", jarPath), e);
      }
    }
    return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
  }

  public static String getProviderDisabledPropertyName(Class<?> clazz) {
    return String.format("ht.filter.provider.%s.disabled", clazz.getSimpleName());
  }

  public static String getProperty(String name) {
    return System.getProperty(name, System.getenv(name.replaceAll("\\.", "_").toUpperCase()));
  }
}
