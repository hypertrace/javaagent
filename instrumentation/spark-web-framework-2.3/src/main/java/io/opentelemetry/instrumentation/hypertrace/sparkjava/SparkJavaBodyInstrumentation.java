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

package io.opentelemetry.instrumentation.hypertrace.sparkjava;

import static io.opentelemetry.javaagent.instrumentation.jetty.JettyHttpServerTracer.TRACER;
import static io.opentelemetry.javaagent.tooling.matcher.NameMatchers.namedOneOf;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletRequest;
import io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletResponse;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import io.opentelemetry.trace.Span;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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
 * {@code Spark.after} is not being called if a handler throws an exception. Exception handler
 * {@code Spark.exception} cannot be used because it overrides user defined exception handlers. This
 * might be fine as on exception there is usually not body send to users.
 */
@AutoService(Instrumenter.class)
public class SparkJavaBodyInstrumentation extends Instrumenter.Default {

  public SparkJavaBodyInstrumentation() {
    super(InstrumentationName.INSTRUMENTATION_NAME[0], InstrumentationName.INSTRUMENTATION_NAME[1]);
  }

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("spark.webserver.MatcherFilter").or(named("spark.http.matching.MatcherFilter"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        namedOneOf("doFilter")
            .and(takesArgument(0, named("javax.servlet.ServletRequest")))
            .and(takesArgument(1, named("javax.servlet.ServletResponse")))
            .and(isPublic()),
        SparkJavaAdvice.class.getName());
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "org.hypertrace.agent.blocking.BlockingProvider",
      "org.hypertrace.agent.blocking.BlockingEvaluator",
      "org.hypertrace.agent.blocking.BlockingResult",
      "org.hypertrace.agent.blocking.ExecutionBlocked",
      "org.hypertrace.agent.blocking.ExecutionNotBlocked",
      "org.hypertrace.agent.blocking.MockBlockingEvaluator",
      "org.hypertrace.agent.core.HypertraceSemanticAttributes",
      "org.hypertrace.agent.core.DynamicConfig",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.ByteBufferData",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.CharBufferData",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.BufferedWriterWrapper",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.BufferedReaderWrapper",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletResponse",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletResponse$BufferingServletOutputStream",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletRequest",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletRequest$ServletInputStreamWrapper",
      packageName + ".InstrumentationName",
    };
  }

  public static class SparkJavaAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class, skipOn = BlockingResult.class)
    public static Object onEnter(
        @Advice.Argument(value = 0, readOnly = false) ServletRequest request,
        @Advice.Argument(value = 1, readOnly = false) ServletResponse response) {
      if (!DynamicConfig.isEnabled(InstrumentationName.INSTRUMENTATION_NAME)) {
        return null;
      }

      if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
        return null;
      }

      Span currentSpan = TRACER.getCurrentSpan();
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      response = new BufferingHttpServletResponse(httpResponse);
      request = new BufferingHttpServletRequest(httpRequest, (HttpServletResponse) response);

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

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static Object onExit(
        @Advice.Argument(value = 0, readOnly = false) ServletRequest request,
        @Advice.Argument(value = 1, readOnly = false) ServletResponse response) {
      if (!(request instanceof BufferingHttpServletRequest)
          || !(response instanceof BufferingHttpServletResponse)) {
        return null;
      }
      BufferingHttpServletResponse bufferingResponse = (BufferingHttpServletResponse) response;
      BufferingHttpServletRequest bufferingRequest = (BufferingHttpServletRequest) request;

      Span currentSpan = TRACER.getCurrentSpan();
      // set response headers
      for (String headerName : bufferingResponse.getHeaderNames()) {
        String headerValue = bufferingResponse.getHeader(headerName);
        currentSpan.setAttribute(
            HypertraceSemanticAttributes.responseHeader(headerName), headerValue);
      }
      // Bodies are captured at the end after all user processing.
      currentSpan.setAttribute(
          HypertraceSemanticAttributes.REQUEST_BODY, bufferingRequest.getBufferedBodyAsString());
      currentSpan.setAttribute(
          HypertraceSemanticAttributes.RESPONSE_BODY, bufferingResponse.getBufferAsString());
      return null;
    }
  }
}
