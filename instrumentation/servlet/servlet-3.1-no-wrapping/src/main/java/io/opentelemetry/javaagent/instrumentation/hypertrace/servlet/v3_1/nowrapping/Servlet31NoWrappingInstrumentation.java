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

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static io.opentelemetry.javaagent.tooling.matcher.NameMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.common.ServletSpanDecorator;
import io.opentelemetry.javaagent.instrumentation.servlet.v3_0.Servlet3HttpServerTracer;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.config.HypertraceConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.filter.FilterRegistry;

public class Servlet31NoWrappingInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(namedOneOf("javax.servlet.Filter", "javax.servlet.http.HttpServlet"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> matchers = new HashMap<>();
    matchers.put(
        namedOneOf("doFilter", "service")
            .and(takesArgument(0, named("javax.servlet.ServletRequest")))
            .and(takesArgument(1, named("javax.servlet.ServletResponse")))
            .and(isPublic()),
        Servlet31NoWrappingInstrumentation.class.getName() + "$ServletAdvice");
    return matchers;
  }

  static class ServletAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class, skipOn = Advice.OnNonDefaultValue.class)
    public static boolean start(
        @Advice.Argument(value = 0) ServletRequest request,
        @Advice.Argument(value = 1) ServletResponse response,
        @Advice.Local("currentSpan") Span currentSpan) {

      int callDepth =
          CallDepthThreadLocalMap.incrementCallDepth(Servlet31NoWrappingInstrumentation.class);
      if (callDepth > 0) {
        return false;
      }
      if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
        return false;
      }

      System.out.println("ServletFilter start");

      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      currentSpan = Java8BytecodeBridge.currentSpan();
      Servlet3HttpServerTracer.getCurrentServerSpan();

      InstrumentationContext.get(HttpServletRequest.class, Span.class)
          .put(httpRequest, currentSpan);

      ServletSpanDecorator.addSessionId(currentSpan, httpRequest);

      // set request headers
      Enumeration<String> headerNames = httpRequest.getHeaderNames();
      Map<String, String> headers = new HashMap<>();
      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        String headerValue = httpRequest.getHeader(headerName);
        AttributeKey<String> attributeKey =
            HypertraceSemanticAttributes.httpRequestHeader(headerName);

        if (HypertraceConfig.get().getDataCapture().getHttpHeaders().getRequest().getValue()) {
          currentSpan.setAttribute(attributeKey, headerValue);
        }
        headers.put(attributeKey.getKey(), headerValue);
      }
      if (FilterRegistry.getFilter().evaluateRequestHeaders(currentSpan, headers)) {
        httpResponse.setStatus(403);
        return true;
      }

      return false;
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.Argument(value = 0) ServletRequest request,
        @Advice.Argument(value = 1) ServletResponse response,
        @Advice.Local("currentSpan") Span currentSpan) {
      int callDepth =
          CallDepthThreadLocalMap.decrementCallDepth(Servlet31NoWrappingInstrumentation.class);
      if (callDepth > 0) {
        return;
      }
      // we are in the most outermost level of Servlet instrumentation

      if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
        return;
      }

      System.out.println("");
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      AgentConfig agentConfig = HypertraceConfig.get();

      AtomicBoolean responseHandled = new AtomicBoolean(false);
      if (request.isAsyncStarted()) {
        try {
          request
              .getAsyncContext()
              .addListener(new BodyCaptureAsyncListener(responseHandled, currentSpan));
        } catch (IllegalStateException e) {
          // org.eclipse.jetty.server.Request may throw an exception here if request became
          // finished after check above. We just ignore that exception and move on.
        }
      }

      if (!request.isAsyncStarted() && responseHandled.compareAndSet(false, true)) {
        if (agentConfig.getDataCapture().getHttpHeaders().getResponse().getValue()) {
          for (String headerName : httpResponse.getHeaderNames()) {
            String headerValue = httpResponse.getHeader(headerName);
            currentSpan.setAttribute(
                HypertraceSemanticAttributes.httpResponseHeader(headerName), headerValue);
          }
        }
      }
    }
  }
}
