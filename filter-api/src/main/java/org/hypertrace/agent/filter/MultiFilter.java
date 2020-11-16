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
import java.util.List;
import java.util.Map;
import org.hypertrace.agent.filter.api.ExecutionBlocked;
import org.hypertrace.agent.filter.api.ExecutionNotBlocked;
import org.hypertrace.agent.filter.api.Filter;
import org.hypertrace.agent.filter.api.FilterResult;

class MultiFilter implements Filter {

  private final List<Filter> filters;

  public MultiFilter(List<Filter> filters) {
    this.filters = filters;
  }

  @Override
  public FilterResult evaluateRequestHeaders(Span span, Map<String, String> headers) {
    boolean shouldBlock = false;
    for (Filter filter : filters) {
      if (filter.evaluateRequestHeaders(span, headers).blockExecution()) {
        shouldBlock = true;
      }
    }
    return shouldBlock ? ExecutionBlocked.INSTANCE : ExecutionNotBlocked.INSTANCE;
  }

  @Override
  public FilterResult evaluateRequestBody(Span span, String body) {
    boolean shouldBlock = false;
    for (Filter filter : filters) {
      if (filter.evaluateRequestBody(span, body).blockExecution()) {
        shouldBlock = true;
      }
    }
    return shouldBlock ? ExecutionBlocked.INSTANCE : ExecutionNotBlocked.INSTANCE;
  }
}
