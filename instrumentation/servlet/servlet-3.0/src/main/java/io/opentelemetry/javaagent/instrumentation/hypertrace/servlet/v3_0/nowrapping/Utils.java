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
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hypertrace.agent.core.bootstrap.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.bootstrap.instrumentation.SpanAndObjectPair;
import org.hypertrace.agent.core.bootstrap.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.bootstrap.instrumentation.buffer.BoundedCharArrayWriter;
import org.hypertrace.agent.core.bootstrap.instrumentation.buffer.ByteBufferSpanPair;
import org.hypertrace.agent.core.bootstrap.instrumentation.buffer.CharBufferSpanPair;

public class Utils {

  private Utils() {}

  public static void addSessionId(Span span, HttpServletRequest httpRequest) {
    if (httpRequest.isRequestedSessionIdValid()) {
      HttpSession session = httpRequest.getSession();
      if (session != null && session.getId() != "") {
        span.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_SESSION_ID, session.getId());
      }
    }
  }

  public static void captureResponseBody(
      Span span,
      HttpServletResponse httpServletResponse,
      ContextStore<HttpServletResponse, SpanAndObjectPair> responseContextStore,
      ContextStore<ServletOutputStream, BoundedByteArrayOutputStream> streamContextStore,
      ContextStore<PrintWriter, BoundedCharArrayWriter> writerContextStore) {

    SpanAndObjectPair responseStreamWriterHolder = responseContextStore.get(httpServletResponse);
    if (responseStreamWriterHolder == null) {
      return;
    }
    responseContextStore.put(httpServletResponse, null);

    if (responseStreamWriterHolder.getAssociatedObject() instanceof ServletOutputStream) {
      ServletOutputStream servletOutputStream =
          (ServletOutputStream) responseStreamWriterHolder.getAssociatedObject();
      BoundedByteArrayOutputStream buffer = streamContextStore.get(servletOutputStream);
      if (buffer != null) {
        try {
          span.setAttribute(
              HypertraceSemanticAttributes.HTTP_RESPONSE_BODY,
              buffer.toStringWithSuppliedCharset());
        } catch (UnsupportedEncodingException e) {
          // should not happen
        }
        streamContextStore.put(servletOutputStream, null);
      }
    } else if (responseStreamWriterHolder.getAssociatedObject() instanceof PrintWriter) {
      PrintWriter printWriter = (PrintWriter) responseStreamWriterHolder.getAssociatedObject();
      BoundedCharArrayWriter buffer = writerContextStore.get(printWriter);
      if (buffer != null) {
        span.setAttribute(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, buffer.toString());
        writerContextStore.put(printWriter, null);
      }
    }
  }

  public static void resetRequestBodyBuffers(
      HttpServletRequest httpServletRequest,
      ContextStore<HttpServletRequest, SpanAndObjectPair> requestContextStore,
      ContextStore<ServletInputStream, ByteBufferSpanPair> streamContextStore,
      ContextStore<BufferedReader, CharBufferSpanPair> bufferedReaderContextStore) {

    SpanAndObjectPair requestStreamReaderHolder = requestContextStore.get(httpServletRequest);
    if (requestContextStore == null) {
      return;
    }
    requestContextStore.put(httpServletRequest, null);

    if (requestStreamReaderHolder.getAssociatedObject() instanceof ServletInputStream) {
      ServletInputStream servletInputStream =
          (ServletInputStream) requestStreamReaderHolder.getAssociatedObject();
      ByteBufferSpanPair byteBufferSpanPair = streamContextStore.get(servletInputStream);
      if (byteBufferSpanPair != null) {
        // capture body explicitly e.g. Jackson does not call ServletInputStream$read() until -1 is
        // returned
        // it does not even call ServletInputStream#available()
        byteBufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
        streamContextStore.put(servletInputStream, null);
      }
    } else if (requestStreamReaderHolder.getAssociatedObject() instanceof BufferedReader) {
      BufferedReader bufferedReader =
          (BufferedReader) requestStreamReaderHolder.getAssociatedObject();
      CharBufferSpanPair charBufferSpanPair = bufferedReaderContextStore.get(bufferedReader);
      if (charBufferSpanPair != null) {
        charBufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
        bufferedReaderContextStore.put(bufferedReader, null);
      }
    }
  }
}
