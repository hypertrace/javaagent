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

package io.opentelemetry.javaagent.instrumentation.hypertrace.undertow.servlet.v1_4;

import static net.bytebuddy.matcher.ElementMatchers.failSafe;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.hypertrace.undertow.common.RequestBodyCaptureMethod;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;

public final class UndertowHttpServletRequestInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return failSafe(named("io.undertow.servlet.spec.HttpServletRequestImpl"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    final Map<Junction<MethodDescription>, String> matchers = new HashMap<>();
    matchers.put(
        named("getInputStream")
            .and(takesArguments(0))
            .and(returns(named("javax.servlet.ServletInputStream")))
            .and(isPublic()),
        UndertowHttpServletRequestInstrumentation.class.getName() + "$ServletBodyCapture_advice");
    matchers.put(
        named("getReader")
            .and(takesArguments(0))
            .and(returns(named("java.io.BufferedReader")))
            .and(isPublic()),
        UndertowHttpServletRequestInstrumentation.class.getName() + "$ServletBodyCapture_advice");
    return matchers;
  }

  /**
   * When {@link HttpServletRequestImpl#getInputStream()} or {@link
   * HttpServletRequestImpl#getReader()} is invoked, we know that we want to capture the request
   * body via our {@link RequestBodyCaptureMethod#SERVLET} approach, rather than via server
   * instrumentation to capture the output of {@link HttpServerExchange#getRequestChannel()} which
   * can be used without the Servlet APIs.
   */
  static final class ServletBodyCapture_advice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(@Advice.This final HttpServletRequestImpl httpServletRequestImpl) {
      final HttpServerExchange exchange = httpServletRequestImpl.getExchange();
      InstrumentationContext.get(HttpServerExchange.class, RequestBodyCaptureMethod.class)
          .put(exchange, RequestBodyCaptureMethod.SERVLET);
    }
  }
}
