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
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;
import org.hypertrace.agent.core.instrumentation.buffer.ByteBufferSpanPair;
import org.hypertrace.agent.core.instrumentation.buffer.CharBufferSpanPair;

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
      ContextStore<ServletOutputStream, BoundedByteArrayOutputStream> streamContextStore,
      ContextStore<PrintWriter, BoundedCharArrayWriter> writerContextStore,
      HttpServletResponse httpResponse) {

    try {
      ServletOutputStream outputStream = httpResponse.getOutputStream();
      BoundedByteArrayOutputStream buffer = streamContextStore.get(outputStream);
      if (buffer != null) {
        span.setAttribute(
            HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, buffer.toStringWithSuppliedCharset());
        streamContextStore.put(outputStream, null);
      }
    } catch (IllegalStateException | IOException exOutStream) {
      // getWriter was called
      try {
        PrintWriter writer = httpResponse.getWriter();
        BoundedCharArrayWriter buffer = writerContextStore.get(writer);
        if (buffer != null) {
          span.setAttribute(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, buffer.toString());
          writerContextStore.put(writer, null);
        }
      } catch (IllegalStateException | IOException exPrintWriter) {
      }
    }
  }

  public static void resetRequestBodyBuffers(
      ContextStore<ServletInputStream, ByteBufferSpanPair> streamContextStore,
      ContextStore<BufferedReader, CharBufferSpanPair> printContextStore,
      HttpServletRequest httpRequest) {
    try {
      ServletInputStream inputStream = httpRequest.getInputStream();
      ByteBufferSpanPair bufferSpanPair = streamContextStore.get(inputStream);
      if (bufferSpanPair != null) {
        streamContextStore.put(inputStream, null);
      }
    } catch (IllegalStateException | IOException exOutStream) {
      // getWriter was called
      try {
        BufferedReader reader = httpRequest.getReader();
        CharBufferSpanPair bufferSpanPair = printContextStore.get(reader);
        if (bufferSpanPair != null) {
          printContextStore.put(reader, null);
        }
      } catch (IllegalStateException | IOException exPrintWriter) {
      }
    }
  }
}
