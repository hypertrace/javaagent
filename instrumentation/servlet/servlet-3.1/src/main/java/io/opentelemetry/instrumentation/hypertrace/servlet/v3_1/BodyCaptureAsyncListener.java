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
      annotateRequest(event.getSuppliedRequest());
      annotateResponse(event.getSuppliedResponse());
    }
  }

  @Override
  public void onError(AsyncEvent event) {
    if (responseHandled.compareAndSet(false, true)) {
      annotateRequest(event.getSuppliedRequest());
      annotateResponse(event.getSuppliedResponse());
    }
  }

  @Override
  public void onTimeout(AsyncEvent event) {}

  @Override
  public void onStartAsync(AsyncEvent event) {}

  private void annotateRequest(ServletRequest servletRequest) {
    if (servletRequest instanceof BufferingHttpServletRequest) {
      BufferingHttpServletRequest bufferingRequest = (BufferingHttpServletRequest) servletRequest;
      span.setAttribute("request.body", bufferingRequest.getBufferedBodyAsString());
    }
  }

  private void annotateResponse(ServletResponse servletResponse) {
    if (servletResponse instanceof BufferingHttpServletResponse) {
      BufferingHttpServletResponse bufferingResponse =
          (BufferingHttpServletResponse) servletResponse;
      String responseBody = bufferingResponse.getBufferAsString();
      span.setAttribute("response.body", responseBody);
      for (String headerName : bufferingResponse.getHeaderNames()) {
        String headerValue = bufferingResponse.getHeader(headerName);
        span.setAttribute("response.header." + headerName, headerValue);
      }
    }
  }
}
