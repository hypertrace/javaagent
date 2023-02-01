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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v2_2.nowrapping;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.field.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceCallDepthThreadLocalMap;
import org.hypertrace.agent.core.instrumentation.HypertraceEvaluationException;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.SpanAndObjectPair;
import org.hypertrace.agent.core.instrumentation.buffer.*;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;
import org.hypertrace.agent.filter.FilterRegistry;

public class Servlet2AndFilterInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("javax.servlet.Servlet");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("javax.servlet.Servlet"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("service")
            .and(takesArgument(0, named("javax.servlet.ServletRequest")))
            .and(takesArgument(1, named("javax.servlet.ServletResponse")))
            .and(isPublic()),
        Servlet2AndFilterInstrumentation.class.getName() + "$ServletAdvice");
  }

  @SuppressWarnings("unused")
  public static class ServletAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class, skipOn = Advice.OnNonDefaultValue.class)
    public static boolean start(
        @Advice.Argument(value = 0) ServletRequest request,
        @Advice.Argument(value = 1) ServletResponse response,
        @Advice.Local("currentSpan") Span currentSpan) {
      System.out.println("start Enter javax.servlet.Servlet.service");
      int callDepth =
          HypertraceCallDepthThreadLocalMap.incrementCallDepth(Servlet2InstrumentationName.class);
      if (callDepth > 0) {
        System.out.println("end1 Enter javax.servlet.Servlet.service");
        return false;
      }
      if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
        System.out.println("end2 Enter javax.servlet.Servlet.service");
        return false;
      }

      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      currentSpan = Java8BytecodeBridge.currentSpan();

      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();

      Utils.addSessionId(currentSpan, httpRequest);

      // set request headers
      Enumeration<?> headerNames = httpRequest.getHeaderNames();
      Map<String, String> headers = new HashMap<>();
      while (headerNames.hasMoreElements()) {
        String headerName;
        try {
          headerName = (String) headerNames.nextElement();
        } catch (Exception e) {
          continue;
        }
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
        System.out.println("end3 Enter javax.servlet.Servlet.service");
        return true;
      }

      if (instrumentationConfig.httpBody().request()
          && ContentTypeUtils.shouldCapture(httpRequest.getContentType())) {
        // The HttpServletRequest instrumentation uses this to
        // enable the instrumentation
        VirtualField.find(HttpServletRequest.class, SpanAndObjectPair.class)
            .set(
                httpRequest,
                new SpanAndObjectPair(currentSpan, Collections.unmodifiableMap(headers)));
      }
      System.out.println("end Enter javax.servlet.Servlet.service");
      return false;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exit(
        @Advice.Argument(0) ServletRequest request,
        @Advice.Argument(1) ServletResponse response,
        @Advice.Thrown(readOnly = false) Throwable throwable,
        @Advice.Local("currentSpan") Span currentSpan) {
      System.out.println("start Exit javax.servlet.Servlet.service");
      int callDepth =
          HypertraceCallDepthThreadLocalMap.decrementCallDepth(Servlet2InstrumentationName.class);
      if (callDepth > 0) {
        System.out.println("end1 Exit javax.servlet.Servlet.service");
        return;
      }
      // we are in the most outermost level of Servlet instrumentation

      if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
        System.out.println("end2 Exit javax.servlet.Servlet.service");
        return;
      }

      HttpServletResponse httpResponse = (HttpServletResponse) response;
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();

      try {
        // response context to capture body and clear the context
        VirtualField<HttpServletResponse, SpanAndObjectPair> responseContextStore =
            VirtualField.find(HttpServletResponse.class, SpanAndObjectPair.class);
        VirtualField<ServletOutputStream, BoundedByteArrayOutputStream> outputStreamContextStore =
            VirtualField.find(ServletOutputStream.class, BoundedByteArrayOutputStream.class);
        VirtualField<PrintWriter, BoundedCharArrayWriter> writerContextStore =
            VirtualField.find(PrintWriter.class, BoundedCharArrayWriter.class);

        // request context to clear body buffer
        VirtualField<HttpServletRequest, SpanAndObjectPair> requestContextStore =
            VirtualField.find(HttpServletRequest.class, SpanAndObjectPair.class);
        VirtualField<ServletInputStream, ByteBufferSpanPair> inputStreamContextStore =
            VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class);
        VirtualField<BufferedReader, CharBufferSpanPair> readerContextStore =
            VirtualField.find(BufferedReader.class, CharBufferSpanPair.class);
        VirtualField<HttpServletRequest, StringMapSpanPair> urlEncodedMapContextStore =
            VirtualField.find(HttpServletRequest.class, StringMapSpanPair.class);

        // capture response body
        // TODO: capture response headers
        // TODO: capture body based on response content type
        if (instrumentationConfig.httpBody().response()) {
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
              httpRequest,
              requestContextStore,
              inputStreamContextStore,
              readerContextStore,
              urlEncodedMapContextStore);
        }
      } finally {
        Throwable tmp = throwable;
        while (tmp != null) { // loop in case our exception is nested (eg. springframework)
          if (tmp instanceof HypertraceEvaluationException) {
            httpResponse.setStatus(403);
            // bytebuddy treats the reassignment of this variable to null as an instruction to
            // suppress this exception, which is what we want
            throwable = null;
            break;
          }
          tmp = tmp.getCause();
        }
      }
      System.out.println("end Exit javax.servlet.Servlet.service");
    }
  }
}
