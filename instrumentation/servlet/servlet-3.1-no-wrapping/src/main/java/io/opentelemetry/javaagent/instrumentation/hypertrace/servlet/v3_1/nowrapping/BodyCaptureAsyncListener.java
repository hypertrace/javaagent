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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping;

import io.opentelemetry.api.trace.Span;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.hypertrace.agent.core.config.HypertraceConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;

public class BodyCaptureAsyncListener implements AsyncListener {

  private final AtomicBoolean responseHandled;
  private final Span span;

  public BodyCaptureAsyncListener(AtomicBoolean responseHandled, Span span) {
    this.responseHandled = responseHandled;
    this.span = span;
  }

  @Override
  public void onComplete(AsyncEvent event) {
    if (responseHandled.compareAndSet(false, true)) {
      captureResponseHeaders(event.getSuppliedResponse());
    }
  }

  @Override
  public void onError(AsyncEvent event) {
    if (responseHandled.compareAndSet(false, true)) {
      captureResponseHeaders(event.getSuppliedResponse());
    }
  }

  @Override
  public void onTimeout(AsyncEvent event) {}

  @Override
  public void onStartAsync(AsyncEvent event) {}

  private void captureResponseHeaders(ServletResponse servletResponse) {
    if (servletResponse instanceof HttpServletResponse) {
      HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
      if (HypertraceConfig.get().getDataCapture().getHttpHeaders().getResponse().getValue()) {
        for (String headerName : httpResponse.getHeaderNames()) {
          String headerValue = httpResponse.getHeader(headerName);
          span.setAttribute(
              HypertraceSemanticAttributes.httpResponseHeader(headerName), headerValue);
        }
      }
    }
  }
}