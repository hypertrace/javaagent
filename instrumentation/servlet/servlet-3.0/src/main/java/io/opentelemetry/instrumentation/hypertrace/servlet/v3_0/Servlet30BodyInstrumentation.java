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

package io.opentelemetry.instrumentation.hypertrace.servlet.v3_0;

import static io.opentelemetry.instrumentation.auto.servlet.v3_0.Servlet3HttpServerTracer.TRACER;
import static io.opentelemetry.javaagent.tooling.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static io.opentelemetry.javaagent.tooling.matcher.NameMatchers.namedOneOf;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import io.opentelemetry.trace.Span;
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
import org.hypertrace.agent.blocking.BlockingProvider;
import org.hypertrace.agent.blocking.BlockingResult;
import org.hypertrace.agent.core.DynamicConfig;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;

/**
 * TODO https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1395 is resolved
 * move this to org.hypertrace package.
 */
@AutoService(Instrumenter.class)
public class Servlet30BodyInstrumentation extends Instrumenter.Default {

  public Servlet30BodyInstrumentation() {
    super(InstrumentationName.INSTRUMENTATION_NAME[0], InstrumentationName.INSTRUMENTATION_NAME[1]);
  }

  @Override
  public int getOrder() {
    /**
     * Order 1 assures that this instrumentation runs after OTEL servlet instrumentation so we can
     * access current span in our advice.
     */
    return 1;
  }

  @Override
  public ElementMatcher<ClassLoader> classLoaderMatcher() {
    // Optimization for expensive typeMatcher.
    // ReadListener was added in 3.1
    return hasClassesNamed("javax.servlet.http.HttpServlet", "javax.servlet.AsyncEvent")
        .and(not(hasClassesNamed("javax.servlet.ReadListener")));
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(
        namedOneOf("javax.servlet.FilterChain", "javax.servlet.http.HttpServlet"));
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "io.opentelemetry.instrumentation.servlet.HttpServletRequestGetter",
      "io.opentelemetry.instrumentation.servlet.ServletHttpServerTracer",
      "io.opentelemetry.instrumentation.auto.servlet.v3_0.Servlet3HttpServerTracer",
      // TODO Add these to bootstrap classloader so they don't have to referenced in every
      // instrumentation, see https://github.com/Traceableai/opentelemetry-javaagent/issues/17
      "org.hypertrace.agent.blocking.BlockingProvider",
      "org.hypertrace.agent.blocking.BlockingEvaluator",
      "org.hypertrace.agent.blocking.BlockingResult",
      "org.hypertrace.agent.blocking.ExecutionBlocked",
      "org.hypertrace.agent.blocking.ExecutionNotBlocked",
      "org.hypertrace.agent.blocking.MockBlockingEvaluator",
      "org.hypertrace.agent.core.HypertraceSemanticAttributes",
      "org.hypertrace.agent.core.DynamicConfig",
      packageName + ".InstrumentationName",
      packageName + ".BufferingHttpServletResponse",
      packageName + ".BufferingHttpServletResponse$BufferingServletOutputStream",
      packageName + ".BufferingHttpServletResponse$BufferedWriterWrapper",
      packageName + ".ByteBufferData",
      packageName + ".CharBufferData",
      packageName + ".BufferingHttpServletRequest",
      packageName + ".BufferingHttpServletRequest$ServletInputStreamWrapper",
      packageName + ".BufferingHttpServletRequest$BufferedReaderWrapper",
      packageName + ".BodyCaptureAsyncListener",
    };
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        namedOneOf("doFilter", "service")
            .and(takesArgument(0, named("javax.servlet.ServletRequest")))
            .and(takesArgument(1, named("javax.servlet.ServletResponse")))
            .and(isPublic()),
        FilterAdvice.class.getName());
  }

  public static class FilterAdvice {
    // request attribute key injected at first filerChain.doFilter
    private static final String ALREADY_LOADED = "__org.hypertrace.agent.on_start_executed";

    @Advice.OnMethodEnter(suppress = Throwable.class, skipOn = BlockingResult.class)
    public static Object start(
        @Advice.Argument(value = 0, readOnly = false) ServletRequest request,
        @Advice.Argument(value = 1, readOnly = false) ServletResponse response,
        @Advice.Local("rootStart") Boolean rootStart) {
      if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
        return null;
      }

      if (!DynamicConfig.isEnabled(InstrumentationName.INSTRUMENTATION_NAME)) {
        return null;
      }

      // TODO run on every doFilter and check if user removed wrapper
      // TODO what if user unwraps request and reads the body?

      // run the instrumentation only for the root FilterChain.doFilter()
      if (request.getAttribute(ALREADY_LOADED) != null) {
        return null;
      }
      request.setAttribute(ALREADY_LOADED, true);

      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      Span currentSpan = TRACER.getCurrentSpan();

      rootStart = true;
      response = new BufferingHttpServletResponse(httpResponse);
      request = new BufferingHttpServletRequest(httpRequest, (HttpServletResponse) response);
      // set request headers
      Enumeration<String> headerNames = httpRequest.getHeaderNames();
      Map<String, String> headers = new HashMap<>();
      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        String headerValue = httpRequest.getHeader(headerName);
        currentSpan.setAttribute(
            HypertraceSemanticAttributes.requestHeader(headerName), headerValue);
        headers.put(headerName, headerValue);
      }
      BlockingResult blockingResult = BlockingProvider.getBlockingEvaluator().evaluate(headers);
      currentSpan.setAttribute(
          HypertraceSemanticAttributes.OPA_RESULT, blockingResult.blockExecution());
      if (blockingResult.blockExecution()) {
        httpResponse.setStatus(403);
        currentSpan.setAttribute(
            HypertraceSemanticAttributes.OPA_REASON, blockingResult.getReason());
        return blockingResult;
      }
      return null;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Argument(0) ServletRequest request,
        @Advice.Argument(1) ServletResponse response,
        @Advice.Local("rootStart") Boolean rootStart) {
      if (rootStart != null) {
        if (!(request instanceof BufferingHttpServletRequest)
            || !(response instanceof BufferingHttpServletResponse)) {
          return;
        }

        request.removeAttribute(ALREADY_LOADED);
        Span currentSpan = TRACER.getCurrentSpan();

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
          BufferingHttpServletResponse bufferingResponse = (BufferingHttpServletResponse) response;
          BufferingHttpServletRequest bufferingRequest = (BufferingHttpServletRequest) request;

          // set response headers
          bufferingResponse.getHeaderNames();
          for (String headerName : bufferingResponse.getHeaderNames()) {
            String headerValue = bufferingResponse.getHeader(headerName);
            currentSpan.setAttribute(
                HypertraceSemanticAttributes.responseHeader(headerName), headerValue);
          }
          // Bodies are captured at the end after all user processing.
          currentSpan.setAttribute(
              HypertraceSemanticAttributes.REQUEST_BODY,
              bufferingRequest.getByteBuffer().getBufferAsString());
          currentSpan.setAttribute(
              HypertraceSemanticAttributes.RESPONSE_BODY, bufferingResponse.getBufferAsString());
        }
      }
    }
  }
}
