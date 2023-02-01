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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v2_2.nowrapping.request;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.*;

import io.opentelemetry.instrumentation.api.field.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
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
import org.hypertrace.agent.core.instrumentation.buffer.StringMapSpanPair;

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
    transformer.applyAdviceToMethod(
        named("getParameter")
            .and(takesArguments(1))
            .and(takesArgument(0, is(String.class)))
            .and(returns(String.class))
            .and(isPublic()),
        ServletRequestInstrumentation.class.getName() + "$ServletRequest_getParameter_advice");
  }

  @SuppressWarnings("unused")
  static class ServletRequest_getInputStream_advice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static SpanAndObjectPair enter(@Advice.This ServletRequest servletRequest) {
      System.out.println("start Enter javax.servlet.ServletRequest.getInputStream");
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
      // span is added in servlet/filter instrumentation if data capture is enabled
      SpanAndObjectPair requestBufferWrapper =
          VirtualField.find(HttpServletRequest.class, SpanAndObjectPair.class)
              .get(httpServletRequest);
      if (requestBufferWrapper == null) {
        System.out.println("end1 Enter javax.servlet.ServletRequest.getInputStream");
        return null;
      }

      // the getReader method might call getInputStream
      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletRequest.class);
      System.out.println("end Enter javax.servlet.ServletRequest.getInputStream");
      return requestBufferWrapper;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletRequest servletRequest,
        @Advice.Return ServletInputStream servletInputStream,
        @Advice.Thrown Throwable throwable,
        @Advice.Enter SpanAndObjectPair spanAndObjectPair) {
      System.out.println("start Exit javax.servlet.ServletRequest.getInputStream");
      if (spanAndObjectPair == null) {
        System.out.println("end1 Exit javax.servlet.ServletRequest.getInputStream");
        return;
      }

      int callDepth = HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletRequest.class);
      if (callDepth > 0) {
        System.out.println("end2 Exit javax.servlet.ServletRequest.getInputStream");
        return;
      }

      if (!(servletRequest instanceof HttpServletRequest) || throwable != null) {
        System.out.println("end3 Exit javax.servlet.ServletRequest.getInputStream");
        return;
      }
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

      VirtualField<ServletInputStream, ByteBufferSpanPair> contextStore =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class);
      if (contextStore.get(servletInputStream) != null) {
        // getInputStream() can be called multiple times
        System.out.println("end4 Exit javax.servlet.ServletRequest.getInputStream");
        return;
      }

      ByteBufferSpanPair bufferSpanPair =
          Utils.createRequestByteBufferSpanPair(
              httpServletRequest, spanAndObjectPair.getSpan(), spanAndObjectPair.getHeaders());
      contextStore.set(servletInputStream, bufferSpanPair);
      spanAndObjectPair.setAssociatedObject(servletInputStream);
      System.out.println("end Exit javax.servlet.ServletRequest.getInputStream");
    }
  }

  @SuppressWarnings("unused")
  static class ServletRequest_getReader_advice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static SpanAndObjectPair enter(@Advice.This ServletRequest servletRequest) {
      System.out.println("start Enter javax.servlet.ServletRequest.getReader");
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
      SpanAndObjectPair spanAndObjectPair =
          VirtualField.find(HttpServletRequest.class, SpanAndObjectPair.class)
              .get(httpServletRequest);
      if (spanAndObjectPair == null) {
        System.out.println("end1 Enter javax.servlet.ServletRequest.getReader");
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletRequest.class);
      System.out.println("end Enter javax.servlet.ServletRequest.getReader");
      return spanAndObjectPair;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletRequest servletRequest,
        @Advice.Return BufferedReader reader,
        @Advice.Thrown Throwable throwable,
        @Advice.Enter SpanAndObjectPair spanAndObjectPair) {
      System.out.println("start Exit javax.servlet.ServletRequest.getReader");
      if (spanAndObjectPair == null) {
        System.out.println("end1 Exit javax.servlet.ServletRequest.getReader");
        return;
      }

      int callDepth = HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletRequest.class);
      if (callDepth > 0) {
        System.out.println("end2 Exit javax.servlet.ServletRequest.getReader");
        return;
      }

      if (!(servletRequest instanceof HttpServletRequest) || throwable != null) {
        System.out.println("end3 Exit javax.servlet.ServletRequest.getReader");
        return;
      }
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

      VirtualField<BufferedReader, CharBufferSpanPair> contextStore =
          VirtualField.find(BufferedReader.class, CharBufferSpanPair.class);
      if (contextStore.get(reader) != null) {
        // getReader() can be called multiple times
        System.out.println("end4 Exit javax.servlet.ServletRequest.getReader");
        return;
      }

      CharBufferSpanPair bufferSpanPair =
          Utils.createRequestCharBufferSpanPair(
              httpServletRequest, spanAndObjectPair.getSpan(), spanAndObjectPair.getHeaders());
      contextStore.set(reader, bufferSpanPair);
      spanAndObjectPair.setAssociatedObject(reader);
      System.out.println("end Exit javax.servlet.ServletRequest.getReader");
    }
  }

  /** Provides instrumentation template for ServletRequest.getParameter() method. */
  @SuppressWarnings("unused")
  static class ServletRequest_getParameter_advice {

    /**
     * Instrumentation template for ServletRequest.getParameter() entry point.
     *
     * @param servletRequest servletRequest
     * @return a (possibly null) SpanAndObjectPair, which will be passed to the method exit
     *     instrumentation
     */
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static SpanAndObjectPair enter(@Advice.This ServletRequest servletRequest) {
      System.out.println("start Enter javax.servlet.ServletRequest.getParameter");
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
      SpanAndObjectPair spanAndObjectPair =
          VirtualField.find(HttpServletRequest.class, SpanAndObjectPair.class)
              .get(httpServletRequest);
      if (spanAndObjectPair == null) {
        System.out.println("end1 Enter javax.servlet.ServletRequest.getParameter");
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletRequest.class);
      System.out.println("end Enter javax.servlet.ServletRequest.getParameter");
      return spanAndObjectPair;
    }

    /**
     * Instrumentation template for ServletRequest.getParameter() exit point(s).
     *
     * @param servletRequest the ServletRequest instance
     * @param returnValue the value that is being returned by getParameter()
     * @param paramName the argument that was passed to getParameter()
     * @param throwable the Throwable object, if exiting method because of a 'throw'
     * @param spanAndObjectPair the value returned by the getParameter() method entry
     *     instrumentation
     */
    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletRequest servletRequest,
        @Advice.Return String returnValue,
        @Advice.Argument(0) String paramName,
        @Advice.Thrown Throwable throwable,
        @Advice.Enter SpanAndObjectPair spanAndObjectPair) {
      System.out.println("start Exit javax.servlet.ServletRequest.getParameter");
      if (spanAndObjectPair == null) {
        System.out.println("end1 Exit javax.servlet.ServletRequest.getParameter");
        return;
      }

      int callDepth = HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletRequest.class);
      if (callDepth > 0) {
        System.out.println("end2 Exit javax.servlet.ServletRequest.getParameter");
        return;
      }

      if (returnValue == null) {
        System.out.println("end3 Exit javax.servlet.ServletRequest.getParameter");
        return;
      }

      if (!(servletRequest instanceof HttpServletRequest) || throwable != null) {
        System.out.println("end4 Exit javax.servlet.ServletRequest.getParameter");
        return;
      }
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

      Map<String, String> stringMap;

      VirtualField<HttpServletRequest, StringMapSpanPair> contextStore =
          VirtualField.find(HttpServletRequest.class, StringMapSpanPair.class);

      StringMapSpanPair stringMapSpanPair = contextStore.get(httpServletRequest);

      if (stringMapSpanPair != null) {
        stringMap = stringMapSpanPair.stringMap;
      } else {
        stringMap = new HashMap<>();
        stringMapSpanPair =
            Utils.createStringMapSpanPair(
                stringMap, spanAndObjectPair.getSpan(), spanAndObjectPair.getHeaders());
        contextStore.set(httpServletRequest, stringMapSpanPair);
      }

      stringMap.put(paramName, returnValue);
      spanAndObjectPair.setAssociatedObject(stringMap);
      System.out.println("end Exit javax.servlet.ServletRequest.getParameter");
    }
  }
}
