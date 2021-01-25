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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.config.HypertraceConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;
import org.hypertrace.agent.core.instrumentation.buffer.ByteBufferSpanPair;
import org.hypertrace.agent.core.instrumentation.buffer.CharBufferSpanPair;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;

public class BodyCaptureAsyncListener implements AsyncListener {

  private final AtomicBoolean responseHandled;
  private final Span span;
  private final ContextStore<ServletOutputStream, BoundedByteArrayOutputStream> streamContextStore;
  private final ContextStore<PrintWriter, BoundedCharArrayWriter> writerContextStore;

  private final ContextStore<ServletInputStream, ByteBufferSpanPair> inputStreamContext;
  private final ContextStore<BufferedReader, CharBufferSpanPair> readerContext;

  private final AgentConfig agentConfig = HypertraceConfig.get();

  public BodyCaptureAsyncListener(
      AtomicBoolean responseHandled,
      Span span,
      ContextStore<ServletOutputStream, BoundedByteArrayOutputStream> streamContextStore,
      ContextStore<PrintWriter, BoundedCharArrayWriter> writerContextStore,
      ContextStore<ServletInputStream, ByteBufferSpanPair> inputStreamContext,
      ContextStore<BufferedReader, CharBufferSpanPair> readerContext) {
    this.responseHandled = responseHandled;
    this.span = span;
    this.streamContextStore = streamContextStore;
    this.writerContextStore = writerContextStore;
    this.inputStreamContext = inputStreamContext;
    this.readerContext = readerContext;
  }

  @Override
  public void onComplete(AsyncEvent event) {
    if (responseHandled.compareAndSet(false, true)) {
      captureResponseData(event.getSuppliedResponse(), event.getSuppliedRequest());
    }
  }

  @Override
  public void onError(AsyncEvent event) {
    if (responseHandled.compareAndSet(false, true)) {
      captureResponseData(event.getSuppliedResponse(), event.getSuppliedRequest());
    }
  }

  @Override
  public void onTimeout(AsyncEvent event) {}

  @Override
  public void onStartAsync(AsyncEvent event) {}

  private void captureResponseData(ServletResponse servletResponse, ServletRequest servletRequest) {
    if (servletResponse instanceof HttpServletResponse) {
      HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

      if (agentConfig.getDataCapture().getHttpBody().getResponse().getValue()
          && ContentTypeUtils.shouldCapture(httpResponse.getContentType())) {
        Utils.captureResponseBody(span, streamContextStore, writerContextStore, httpResponse);
      }

      if (agentConfig.getDataCapture().getHttpHeaders().getResponse().getValue()) {
        for (String headerName : httpResponse.getHeaderNames()) {
          String headerValue = httpResponse.getHeader(headerName);
          span.setAttribute(
              HypertraceSemanticAttributes.httpResponseHeader(headerName), headerValue);
        }
      }
    }
    if (servletRequest instanceof HttpServletRequest) {
      HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

      // remove request body buffers from context stores, otherwise they might get reused
      if (agentConfig.getDataCapture().getHttpBody().getRequest().getValue()
          && ContentTypeUtils.shouldCapture(httpRequest.getContentType())) {
        Utils.resetRequestBodyBuffers(inputStreamContext, readerContext, httpRequest);
      }
    }
  }
}
