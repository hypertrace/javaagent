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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.async;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.Utils;
import io.opentelemetry.javaagent.instrumentation.servlet.ServletAsyncListener;
import io.opentelemetry.javaagent.instrumentation.servlet.v3_0.Servlet3Singletons;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.SpanAndObjectPair;
import org.hypertrace.agent.core.instrumentation.buffer.*;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;

public final class BodyCaptureAsyncListener implements ServletAsyncListener<HttpServletResponse> {

  private static final InstrumentationConfig instrumentationConfig =
      InstrumentationConfig.ConfigProvider.get();

  private final AtomicBoolean responseHandled;
  private final Span span;

  private final VirtualField<HttpServletResponse, SpanAndObjectPair> responseContextStore;
  private final VirtualField<ServletOutputStream, BoundedByteArrayOutputStream> streamContextStore;
  private final VirtualField<PrintWriter, BoundedCharArrayWriter> writerContextStore;

  private final VirtualField<HttpServletRequest, SpanAndObjectPair> requestContextStore;
  private final VirtualField<ServletInputStream, ByteBufferSpanPair> inputStreamContextStore;
  private final VirtualField<BufferedReader, CharBufferSpanPair> readerContextStore;
  private final VirtualField<HttpServletRequest, StringMapSpanPair> urlEncodedMapContextStore;
  private final HttpServletRequest request;

  public BodyCaptureAsyncListener(
      AtomicBoolean responseHandled,
      VirtualField<HttpServletResponse, SpanAndObjectPair> responseContextStore,
      VirtualField<ServletOutputStream, BoundedByteArrayOutputStream> streamContextStore,
      VirtualField<PrintWriter, BoundedCharArrayWriter> writerContextStore,
      VirtualField<HttpServletRequest, SpanAndObjectPair> requestContextStore,
      VirtualField<ServletInputStream, ByteBufferSpanPair> inputStreamContextStore,
      VirtualField<BufferedReader, CharBufferSpanPair> readerContextStore,
      VirtualField<HttpServletRequest, StringMapSpanPair> urlEncodedMapContextStore,
      HttpServletRequest request) {
    this.responseHandled = responseHandled;
    this.span = Span.fromContext(Servlet3Singletons.helper().getServerContext(request));
    this.responseContextStore = responseContextStore;
    this.streamContextStore = streamContextStore;
    this.writerContextStore = writerContextStore;
    this.requestContextStore = requestContextStore;
    this.inputStreamContextStore = inputStreamContextStore;
    this.readerContextStore = readerContextStore;
    this.urlEncodedMapContextStore = urlEncodedMapContextStore;
    this.request = request;
  }

  @Override
  public void onComplete(HttpServletResponse response) {
    if (responseHandled.compareAndSet(false, true)) {
      captureResponseDataAndClearRequestBuffer(response, request);
    }
  }

  @Override
  public void onError(Throwable throwable, HttpServletResponse response) {
    if (responseHandled.compareAndSet(false, true)) {
      captureResponseDataAndClearRequestBuffer(response, request);
    }
  }

  @Override
  public void onTimeout(long timeout) {
    // noop
  }

  private void captureResponseDataAndClearRequestBuffer(
      HttpServletResponse servletResponse, HttpServletRequest servletRequest) {
    if (servletResponse != null) {
      if (instrumentationConfig.httpBody().response()
          && ContentTypeUtils.shouldCapture(servletResponse.getContentType())) {
        Utils.captureResponseBody(
            span, servletResponse, responseContextStore, streamContextStore, writerContextStore);
      }

      if (instrumentationConfig.httpHeaders().response()) {
        for (String headerName : servletResponse.getHeaderNames()) {
          String headerValue = servletResponse.getHeader(headerName);
          span.setAttribute(
              HypertraceSemanticAttributes.httpResponseHeader(headerName), headerValue);
        }
      }
    }
    if (servletRequest != null) {
      // remove request body buffers from context stores, otherwise they might get reused
      if (instrumentationConfig.httpBody().request()
          && ContentTypeUtils.shouldCapture(servletRequest.getContentType())) {
        Utils.resetRequestBodyBuffers(
            servletRequest,
            requestContextStore,
            inputStreamContextStore,
            readerContextStore,
            urlEncodedMapContextStore);
      }
    }
  }
}
