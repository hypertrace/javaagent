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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.request;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import java.io.BufferedReader;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.HypertraceCallDepthThreadLocalMap;
import org.hypertrace.agent.core.instrumentation.SpanAndObjectPair;
import org.hypertrace.agent.core.instrumentation.buffer.ByteBufferSpanPair;
import org.hypertrace.agent.core.instrumentation.buffer.CharBufferSpanPair;

public class ServletRequestInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("javax.servlet.ServletRequest"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("getInputStream")
            .and(takesArguments(0))
            .and(returns(named("javax.servlet.ServletInputStream")))
            .and(isPublic()),
        ServletRequestInstrumentation.class.getName() + "$ServletRequest_getInputStream_advice");
    transformer.applyAdviceToMethod(
        named("getReader")
            .and(takesArguments(0))
            //            .and(returns(BufferedReader.class))
            .and(isPublic()),
        ServletRequestInstrumentation.class.getName() + "$ServletRequest_getReader_advice");
  }

  static class ServletRequest_getInputStream_advice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static SpanAndObjectPair enter(@Advice.This ServletRequest servletRequest) {
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
      // span is added in servlet/filter instrumentation if data capture is enabled
      SpanAndObjectPair requestBufferWrapper =
          InstrumentationContext.get(HttpServletRequest.class, SpanAndObjectPair.class)
              .get(httpServletRequest);
      if (requestBufferWrapper == null) {
        return null;
      }

      // the getReader method might call getInputStream
      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletRequest.class);
      return requestBufferWrapper;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletRequest servletRequest,
        @Advice.Return ServletInputStream servletInputStream,
        @Advice.Thrown Throwable throwable,
        @Advice.Enter SpanAndObjectPair spanAndObjectPair) {

      if (spanAndObjectPair == null) {
        return;
      }

      int callDepth = HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletRequest.class);
      if (callDepth > 0) {
        return;
      }

      if (!(servletRequest instanceof HttpServletRequest) || throwable != null) {
        return;
      }
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

      ContextStore<ServletInputStream, ByteBufferSpanPair> contextStore =
          InstrumentationContext.get(ServletInputStream.class, ByteBufferSpanPair.class);
      if (contextStore.get(servletInputStream) != null) {
        // getInputStream() can be called multiple times
        return;
      }

      ByteBufferSpanPair bufferSpanPair =
          Utils.createRequestByteBufferSpanPair(
              httpServletRequest, spanAndObjectPair.getSpan(), spanAndObjectPair.getHeaders());
      contextStore.put(servletInputStream, bufferSpanPair);
      spanAndObjectPair.setAssociatedObject(servletInputStream);
    }
  }

  static class ServletRequest_getReader_advice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static SpanAndObjectPair enter(@Advice.This ServletRequest servletRequest) {
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
      SpanAndObjectPair spanAndObjectPair =
          InstrumentationContext.get(HttpServletRequest.class, SpanAndObjectPair.class)
              .get(httpServletRequest);
      if (spanAndObjectPair == null) {
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletRequest.class);
      return spanAndObjectPair;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletRequest servletRequest,
        @Advice.Return BufferedReader reader,
        @Advice.Thrown Throwable throwable,
        @Advice.Enter SpanAndObjectPair spanAndObjectPair) {

      if (spanAndObjectPair == null) {
        return;
      }

      int callDepth = HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletRequest.class);
      if (callDepth > 0) {
        return;
      }

      if (!(servletRequest instanceof HttpServletRequest) || throwable != null) {
        return;
      }
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

      ContextStore<BufferedReader, CharBufferSpanPair> contextStore =
          InstrumentationContext.get(BufferedReader.class, CharBufferSpanPair.class);
      if (contextStore.get(reader) != null) {
        // getReader() can be called multiple times
        return;
      }

      CharBufferSpanPair bufferSpanPair =
          Utils.createRequestCharBufferSpanPair(httpServletRequest, spanAndObjectPair.getSpan());
      contextStore.put(reader, bufferSpanPair);
      spanAndObjectPair.setAssociatedObject(reader);
    }
  }
}
