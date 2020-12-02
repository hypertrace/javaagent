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

package io.opentelemetry.instrumentation.hypertrace.servlet.v2_3;

import static io.opentelemetry.javaagent.tooling.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static io.opentelemetry.javaagent.tooling.matcher.NameMatchers.namedOneOf;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.hypertrace.servlet.common.ServletSpanDecorator;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.HypertraceConfig;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.hypertrace.agent.filter.FilterRegistry;

/**
 * Body capture for servlet 2.3. Note that OTEL servlet instrumentation is compatible with servlet
 * version 2.2, however this implementation uses request and response wrappers that were introduced
 * in 2.3.
 */
@AutoService(InstrumentationModule.class)
public class Servlet2BodyInstrumentationModule extends InstrumentationModule {

  public Servlet2BodyInstrumentationModule() {
    super(Servlet2InstrumentationName.PRIMARY, Servlet2InstrumentationName.OTHER);
  }

  @Override
  public int getOrder() {
    return 1;
  }

  // this is required to make sure servlet 2 instrumentation won't apply to servlet 3
  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    // Request/response wrappers are available since servlet 2.3!
    return hasClassesNamed(
            "javax.servlet.http.HttpServlet", "javax.servlet.http.HttpServletRequestWrapper")
        .and(not(hasClassesNamed("javax.servlet.AsyncEvent", "javax.servlet.AsyncListener")));
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap("javax.servlet.ServletResponse", Integer.class.getName());
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new Servlet2BodyInstrumentation());
  }

  public static class Servlet2BodyInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return safeHasSuperType(namedOneOf("javax.servlet.Filter", "javax.servlet.http.HttpServlet"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      return singletonMap(
          namedOneOf("doFilter", "service")
              .and(takesArgument(0, named("javax.servlet.ServletRequest")))
              .and(takesArgument(1, named("javax.servlet.ServletResponse")))
              .and(isPublic()),
          Filter2Advice.class.getName());
    }
  }

  public static class Filter2Advice {
    // request attribute key injected at first filerChain.doFilter
    private static final String ALREADY_LOADED = "__org.hypertrace.agent.on_start_executed";

    @Advice.OnMethodEnter(suppress = Throwable.class, skipOn = Advice.OnNonDefaultValue.class)
    public static boolean start(
        @Advice.Argument(value = 0, readOnly = false) ServletRequest request,
        @Advice.Argument(value = 1, readOnly = false) ServletResponse response,
        @Advice.Local("rootStart") boolean rootStart) {

      if (!HypertraceConfig.isInstrumentationEnabled(
          Servlet2InstrumentationName.PRIMARY, Servlet2InstrumentationName.OTHER)) {
        return false;
      }
      if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
        return false;
      }

      // TODO run on every doFilter and check if user removed wrapper
      // TODO what if user unwraps request and reads the body?

      // run the instrumentation only for the root FilterChain.doFilter()
      if (request.getAttribute(ALREADY_LOADED) != null) {
        return false;
      }
      request.setAttribute(ALREADY_LOADED, true);

      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      Span currentSpan = Java8BytecodeBridge.currentSpan();

      rootStart = true;

      if (!HypertraceConfig.disableServletWrapperTypes()) {
        response = new BufferingHttpServletResponse(httpResponse);
        request = new BufferingHttpServletRequest(httpRequest, (HttpServletResponse) response);
      }

      ServletSpanDecorator.addSessionId(currentSpan, httpRequest);

      // set request headers
      @SuppressWarnings("unchecked")
      Enumeration<String> headerNames = httpRequest.getHeaderNames();
      Map<String, String> headers = new HashMap<>();

      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        String headerValue = httpRequest.getHeader(headerName);
        if (HypertraceConfig.get().getDataCapture().getHttpHeaders().getRequest().getValue()) {
          currentSpan.setAttribute(
              HypertraceSemanticAttributes.httpRequestHeader(headerName), headerValue);
        }
        headers.put(headerName, headerValue);
      }
      boolean block = FilterRegistry.getFilter().evaluateRequestHeaders(currentSpan, headers);
      if (block) {
        httpResponse.setStatus(403);
        return true;
      }
      return false;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Argument(0) ServletRequest request,
        @Advice.Argument(1) ServletResponse response,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("rootStart") boolean rootStart) {

      HypertraceConfig.recordException(throwable);

      if (!rootStart
          || !(request instanceof BufferingHttpServletRequest)
          || !(response instanceof BufferingHttpServletResponse)) {
        return;
      }

      if (!(request instanceof BufferingHttpServletRequest)
          || !(response instanceof BufferingHttpServletResponse)) {
        return;
      }

      request.removeAttribute(ALREADY_LOADED);
      Span currentSpan = Java8BytecodeBridge.currentSpan();

      BufferingHttpServletResponse bufferingResponse = (BufferingHttpServletResponse) response;
      BufferingHttpServletRequest bufferingRequest = (BufferingHttpServletRequest) request;

      if (HypertraceConfig.get().getDataCapture().getHttpHeaders().getResponse().getValue()) {
        // set response headers
        Map<String, List<String>> bufferedHeaders = bufferingResponse.getBufferedHeaders();
        for (Map.Entry<String, List<String>> nameToHeadersEntry : bufferedHeaders.entrySet()) {
          String headerName = nameToHeadersEntry.getKey();
          for (String headerValue : nameToHeadersEntry.getValue()) {
            currentSpan.setAttribute(
                HypertraceSemanticAttributes.httpResponseHeader(headerName), headerValue);
          }
        }
      }
      // Bodies are captured at the end after all user processing.
      if (HypertraceConfig.get().getDataCapture().getHttpBody().getRequest().getValue()) {
        currentSpan.setAttribute(
            HypertraceSemanticAttributes.HTTP_REQUEST_BODY,
            bufferingRequest.getBufferedBodyAsString());
      }
      if (HypertraceConfig.get().getDataCapture().getHttpBody().getResponse().getValue()) {
        currentSpan.setAttribute(
            HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, bufferingResponse.getBufferAsString());
      }
    }
  }
}
