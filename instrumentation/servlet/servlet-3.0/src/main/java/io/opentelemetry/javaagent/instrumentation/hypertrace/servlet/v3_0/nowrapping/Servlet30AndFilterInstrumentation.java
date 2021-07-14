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

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.safeHasSuperType;
import static io.opentelemetry.javaagent.extension.matcher.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.NameMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.SpanAndObjectPair;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;
import org.hypertrace.agent.core.instrumentation.buffer.ByteBufferSpanPair;
import org.hypertrace.agent.core.instrumentation.buffer.CharBufferSpanPair;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;
import org.hypertrace.agent.filter.FilterRegistry;

public class Servlet30AndFilterInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("javax.servlet.Filter");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return safeHasSuperType(namedOneOf("javax.servlet.Filter", "javax.servlet.Servlet"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> matchers = new HashMap<>();
    matchers.put(
        namedOneOf("doFilter", "service")
            .and(takesArgument(0, named("javax.servlet.ServletRequest")))
            .and(takesArgument(1, named("javax.servlet.ServletResponse")))
            .and(isPublic()),
        Servlet30AndFilterInstrumentation.class.getName() + "$ServletAdvice");
    return matchers;
  }

  public static class ServletAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class, skipOn = Advice.OnNonDefaultValue.class)
    public static boolean start(
        @Advice.Argument(value = 0) ServletRequest request,
        @Advice.Argument(value = 1) ServletResponse response,
        @Advice.Local("currentSpan") Span currentSpan) {

      int callDepth =
          CallDepthThreadLocalMap.incrementCallDepth(Servlet30InstrumentationName.class);
      if (callDepth > 0) {
        return false;
      }
      if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
        return false;
      }

      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      currentSpan = Java8BytecodeBridge.currentSpan();

      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();

      String contentType = httpRequest.getContentType();
      if (instrumentationConfig.httpBody().request()
          && ContentTypeUtils.shouldCapture(contentType)) {
        // The HttpServletRequest instrumentation uses this to
        // enable the instrumentation
        InstrumentationContext.get(HttpServletRequest.class, SpanAndObjectPair.class)
            .put(httpRequest, new SpanAndObjectPair(currentSpan));
      }

      Utils.addSessionId(currentSpan, httpRequest);

      // set request headers
      Enumeration<String> headerNames = httpRequest.getHeaderNames();
      Map<String, String> headers = new HashMap<>();
      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        String headerValue = httpRequest.getHeader(headerName);
        AttributeKey<String> attributeKey =
            HypertraceSemanticAttributes.httpRequestHeader(headerName);

        if (instrumentationConfig.httpHeaders().request()) {
          currentSpan.setAttribute(attributeKey, headerValue);
        }
        headers.put(attributeKey.getKey(), headerValue);
      }

      if (FilterRegistry.getFilter().evaluateRequestHeaders(currentSpan, headers)) {
        httpResponse.setStatus(403);
        // skip execution of the user code
        return true;
      }
      return false;
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.Argument(0) ServletRequest request,
        @Advice.Argument(1) ServletResponse response,
        @Advice.Local("currentSpan") Span currentSpan) {
      int callDepth =
          CallDepthThreadLocalMap.decrementCallDepth(Servlet30InstrumentationName.class);
      if (callDepth > 0) {
        return;
      }
      // we are in the most outermost level of Servlet instrumentation

      if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
        return;
      }

      HttpServletResponse httpResponse = (HttpServletResponse) response;
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();

      // response context to capture body and clear the context
      ContextStore<HttpServletResponse, SpanAndObjectPair> responseContextStore =
          InstrumentationContext.get(HttpServletResponse.class, SpanAndObjectPair.class);
      ContextStore<ServletOutputStream, BoundedByteArrayOutputStream> outputStreamContextStore =
          InstrumentationContext.get(ServletOutputStream.class, BoundedByteArrayOutputStream.class);
      ContextStore<PrintWriter, BoundedCharArrayWriter> writerContextStore =
          InstrumentationContext.get(PrintWriter.class, BoundedCharArrayWriter.class);

      // request context to clear body buffer
      ContextStore<HttpServletRequest, SpanAndObjectPair> requestContextStore =
          InstrumentationContext.get(HttpServletRequest.class, SpanAndObjectPair.class);
      ContextStore<ServletInputStream, ByteBufferSpanPair> inputStreamContextStore =
          InstrumentationContext.get(ServletInputStream.class, ByteBufferSpanPair.class);
      ContextStore<BufferedReader, CharBufferSpanPair> readerContextStore =
          InstrumentationContext.get(BufferedReader.class, CharBufferSpanPair.class);

      AtomicBoolean responseHandled = new AtomicBoolean(false);
      if (request.isAsyncStarted()) {
        try {
          request
              .getAsyncContext()
              .addListener(
                  new BodyCaptureAsyncListener(
                      responseHandled,
                      currentSpan,
                      responseContextStore,
                      outputStreamContextStore,
                      writerContextStore,
                      requestContextStore,
                      inputStreamContextStore,
                      readerContextStore));
        } catch (IllegalStateException e) {
          // org.eclipse.jetty.server.Request may throw an exception here if request became
          // finished after check above. We just ignore that exception and move on.
        }
      }

      if (!request.isAsyncStarted() && responseHandled.compareAndSet(false, true)) {
        if (instrumentationConfig.httpHeaders().response()) {
          for (String headerName : httpResponse.getHeaderNames()) {
            String headerValue = httpResponse.getHeader(headerName);
            currentSpan.setAttribute(
                HypertraceSemanticAttributes.httpResponseHeader(headerName), headerValue);
          }
        }

        // capture response body
        if (instrumentationConfig.httpBody().response()
            && ContentTypeUtils.shouldCapture(httpResponse.getContentType())) {
          Utils.captureResponseBody(
              currentSpan,
              httpResponse,
              responseContextStore,
              outputStreamContextStore,
              writerContextStore);
        }

        // remove request body buffers from context stores, otherwise they might get reused
        if (instrumentationConfig.httpBody().request()
            && ContentTypeUtils.shouldCapture(httpRequest.getContentType())) {
          Utils.resetRequestBodyBuffers(
              httpRequest, requestContextStore, inputStreamContextStore, readerContextStore);
        }
      }
    }
  }
}
