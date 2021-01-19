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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.request;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.ByteBufferMetadata;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.core.instrumentation.buffer.CharBufferAndSpan;

public class ServletRequestInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(named("javax.servlet.ServletRequest"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> matchers = new HashMap<>();
    matchers.put(
        named("getInputStream")
            .and(takesArguments(0))
            .and(returns(named("javax.servlet.ServletInputStream")))
            .and(isPublic()),
        ServletRequestInstrumentation.class.getName() + "$ServletRequest_getInputStream_advice");
    matchers.put(
        named("getReader")
            .and(takesArguments(0))
            //            .and(returns(BufferedReader.class))
            .and(isPublic()),
        ServletRequestInstrumentation.class.getName() + "$ServletRequest_getReader_advice");
    return matchers;
  }

  static class ServletRequest_getInputStream_advice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter() {
      // the getReader method might call getInputStream
      CallDepthThreadLocalMap.incrementCallDepth(ServletRequest.class);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletRequest servletRequest,
        @Advice.Return ServletInputStream servletInputStream) {

      int callDepth = CallDepthThreadLocalMap.decrementCallDepth(ServletRequest.class);
      if (callDepth > 0) {
        return;
      }

      if (!(servletRequest instanceof HttpServletRequest)) {
        return;
      }
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

      ContextStore<ServletInputStream, ByteBufferMetadata> contextStore =
          InstrumentationContext.get(ServletInputStream.class, ByteBufferMetadata.class);
      if (contextStore.get(servletInputStream) != null) {
        // getInputStream() can be called multiple times
        return;
      }

      // span is added in servlet/filter instrumentation if data capture is enabled and it is the
      // right content
      Span requestSpan =
          InstrumentationContext.get(HttpServletRequest.class, Span.class).get(httpServletRequest);
      if (requestSpan == null) {
        return;
      }

      System.out.println("getting servlet request inputStream");

      ByteBufferMetadata metadata =
          HttpRequestInstrumentationUtils.createRequestByteBufferMetadata(
              httpServletRequest, requestSpan);
      contextStore.put(servletInputStream, metadata);
    }
  }

  static class ServletRequest_getReader_advice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter() {
      CallDepthThreadLocalMap.incrementCallDepth(ServletRequest.class);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletRequest servletRequest, @Advice.Return BufferedReader reader) {

      int callDepth = CallDepthThreadLocalMap.decrementCallDepth(ServletRequest.class);
      if (callDepth > 0) {
        return;
      }

      if (!(servletRequest instanceof HttpServletRequest)) {
        return;
      }
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

      ContextStore<BufferedReader, CharBufferAndSpan> contextStore =
          InstrumentationContext.get(BufferedReader.class, CharBufferAndSpan.class);
      if (contextStore.get(reader) != null) {
        // getReader() can be called multiple times
        return;
      }

      // span is added in servlet/filter instrumentation if data capture is enabled and it is the
      // right content
      Span requestSpan =
          InstrumentationContext.get(HttpServletRequest.class, Span.class).get(httpServletRequest);
      if (requestSpan == null) {
        return;
      }

      System.out.println("Adding BufferedReader to context");
      CharBufferAndSpan metadata =
          HttpRequestInstrumentationUtils.createRequestCharBufferMetadata(
              httpServletRequest, requestSpan);
      contextStore.put(reader, metadata);
      System.out.println("getting servlet request bufferedReader ---> end");
    }
  }
}
