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

package org.hypertrace.agent.testing.mockfilter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.Map;
import org.hypertrace.agent.core.filter.FilterResult;
import org.hypertrace.agent.filter.api.Filter;

/** Mock filter, blocks execution if an attribute with "mockblock" key is present. */
class MockFilter implements Filter {

  MockFilter() {}

  @Override
  public FilterResult evaluateRequestHeaders(Span span, Map<String, String> headers) {
    if (headers.containsKey("http.request.header.mockblock")
        || headers.containsKey("rpc.request.metadata.mockblock")) {
      span.setAttribute("hypertrace.mock.filter.result", "true");
      return new FilterResult(true, 403, "Hypertrace Blocked Request");
    }
    return new FilterResult(false, 403, "Hypertrace Blocked Request");
  }

  @Override
  public FilterResult evaluateRequestBody(Span span, String body, Map<String, String> headers) {
    if (body != null && body.contains("block=true")) {
      return new FilterResult(true, 403, "Hypertrace Blocked Request");
    }
    if (span instanceof ReadableSpan) {
      String spanReqBody =
          ((ReadableSpan) span).getAttribute(AttributeKey.stringKey("http.request.body"));
      boolean shouldBlock = spanReqBody != null && spanReqBody.contains("block=true");
      return new FilterResult(shouldBlock, 403, "Hypertrace Blocked Request");
    }
    return new FilterResult(false, 403, "Hypertrace Blocked Request");
  }
}
