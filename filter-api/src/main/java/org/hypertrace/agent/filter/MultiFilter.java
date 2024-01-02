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

import io.opentelemetry.api.trace.Span;
import java.util.List;
import java.util.Map;
import org.hypertrace.agent.core.filter.FilterResult;
import org.hypertrace.agent.filter.api.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MultiFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(MultiFilter.class);

  private final List<Filter> filters;

  public MultiFilter(List<Filter> filters) {
    this.filters = filters;
  }

  @Override
  public FilterResult evaluateRequestHeaders(Span span, Map<String, String> headers) {
    for (Filter filter : filters) {
      try {
        FilterResult filterResult = filter.evaluateRequestHeaders(span, headers);
        if (filterResult.shouldBlock()) {
          return filterResult;
        }
      } catch (Throwable t) {
        logger.warn(
            "Throwable thrown while evaluating Request headers for filter {}",
            filter.getClass().getName(),
            t);
      }
    }
    return new FilterResult(false, 0, "");
  }

  @Override
  public FilterResult evaluateRequestBody(Span span, String body, Map<String, String> headers) {
    for (Filter filter : filters) {
      try {
        FilterResult filterResult = filter.evaluateRequestBody(span, body, headers);
        if (filterResult.shouldBlock()) {
          return filterResult;
        }
      } catch (Throwable t) {
        logger.warn(
            "Throwable thrown while evaluating Request body for filter {}",
            filter.getClass().getName(),
            t);
      }
    }
    return new FilterResult(false, 0, "");
  }
}
