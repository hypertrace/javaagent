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

package io.opentelemetry.instrumentation.hypertrace.servlet.v3_1;

import io.opentelemetry.trace.Span;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.hypertrace.agent.core.HypertraceConfig;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;

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
      captureRequestBody(event.getSuppliedRequest());
      captureResponseBodyAndHeaders(event.getSuppliedResponse());
    }
  }

  @Override
  public void onError(AsyncEvent event) {
    if (responseHandled.compareAndSet(false, true)) {
      captureRequestBody(event.getSuppliedRequest());
      captureResponseBodyAndHeaders(event.getSuppliedResponse());
    }
  }

  @Override
  public void onTimeout(AsyncEvent event) {}

  @Override
  public void onStartAsync(AsyncEvent event) {}

  private void captureRequestBody(ServletRequest servletRequest) {
    if (HypertraceConfig.get().getDataCapture().getHttpBody().getRequest().getValue()) {
      if (servletRequest instanceof BufferingHttpServletRequest) {
        BufferingHttpServletRequest bufferingRequest = (BufferingHttpServletRequest) servletRequest;
        span.setAttribute(
            HypertraceSemanticAttributes.HTTP_REQUEST_BODY,
            bufferingRequest.getBufferedBodyAsString());
      }
    }
  }

  private void captureResponseBodyAndHeaders(ServletResponse servletResponse) {
    if (servletResponse instanceof BufferingHttpServletResponse) {
      BufferingHttpServletResponse bufferingResponse =
          (BufferingHttpServletResponse) servletResponse;
      if (HypertraceConfig.get().getDataCapture().getHttpBody().getResponse().getValue()) {
        String responseBody = bufferingResponse.getBufferAsString();
        span.setAttribute(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, responseBody);
      }
      if (HypertraceConfig.get().getDataCapture().getHttpHeaders().getResponse().getValue()) {
        for (String headerName : bufferingResponse.getHeaderNames()) {
          String headerValue = bufferingResponse.getHeader(headerName);
          span.setAttribute(
              HypertraceSemanticAttributes.httpResponseHeader(headerName), headerValue);
        }
      }
    }
  }
}
