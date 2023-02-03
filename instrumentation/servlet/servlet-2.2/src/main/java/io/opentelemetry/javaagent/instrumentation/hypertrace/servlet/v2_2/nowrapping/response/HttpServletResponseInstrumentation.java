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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v2_2.nowrapping.response;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.*;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.sdk.trace.ReadableSpan;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;

public class HttpServletResponseInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("javax.servlet.http.HttpServletResponse"));
  }
  /*
   * public void setStatus(int sc);
   * public void setStatus(int sc, String sm);
   */
  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("setHeader")
            .and(takesArguments(2))
            .and(takesArgument(0, is(String.class)))
            .and(takesArgument(1, is(String.class)))
            .and(returns(void.class))
            .and(isPublic()),
        HttpServletResponseInstrumentation.class.getName() + "$HttpServletResponse_setHeader");
    transformer.applyAdviceToMethod(
        named("addHeader")
            .and(takesArguments(2))
            .and(takesArgument(0, is(String.class)))
            .and(takesArgument(1, is(String.class)))
            .and(returns(void.class))
            .and(isPublic()),
        HttpServletResponseInstrumentation.class.getName() + "$HttpServletResponse_addHeader");
    for (String methodName : new String[] {"setDateHeader", "addDateHeader"}) {
      transformer.applyAdviceToMethod(
          named(methodName)
              .and(takesArguments(2))
              .and(takesArgument(0, is(String.class)))
              .and(takesArgument(1, is(long.class)))
              .and(returns(void.class))
              .and(isPublic()),
          HttpServletResponseInstrumentation.class.getName()
              + "$HttpServletResponse_setDateHeader");
    }
    for (String methodName : new String[] {"setIntHeader", "addIntHeader"}) {
      transformer.applyAdviceToMethod(
          named(methodName)
              .and(takesArguments(2))
              .and(takesArgument(0, is(String.class)))
              .and(takesArgument(1, is(int.class)))
              .and(returns(void.class))
              .and(isPublic()),
          HttpServletResponseInstrumentation.class.getName() + "$HttpServletResponse_setIntHeader");
    }
  }

  @SuppressWarnings("unused")
  static class HttpServletResponse_setHeader {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This HttpServletResponse httpServletResponse,
        @Advice.Argument(value = 0) String headerName,
        @Advice.Argument(value = 1) String headerValue) {
      System.out.printf(
          "start Enter javax.servlet.http.HttpServletResponse.setHeader (%s, %s)\n",
          headerName, headerValue);
      Span currentSpan = Java8BytecodeBridge.currentSpan();
      AttributeKey<String> attributeKey =
          HypertraceSemanticAttributes.httpResponseHeader(headerName);
      currentSpan.setAttribute(attributeKey, headerValue);
      System.out.println("end Enter javax.servlet.http.HttpServletResponse.setHeader");
    }
  }

  @SuppressWarnings("unused")
  static class HttpServletResponse_addHeader {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This HttpServletResponse httpServletResponse,
        @Advice.Argument(value = 0) String headerName,
        @Advice.Argument(value = 1) String headerValue) {
      System.out.printf(
          "start Enter javax.servlet.http.HttpServletResponse.addHeader (%s, %s)\n",
          headerName, headerValue);
      Span currentSpan = Java8BytecodeBridge.currentSpan();
      if (!(currentSpan instanceof ReadableSpan)) {
        return;
      }
      AttributeKey<String> attributeKey =
          HypertraceSemanticAttributes.httpResponseHeader(headerName);
      String oldHeaderValue = ((ReadableSpan) currentSpan).getAttribute(attributeKey);
      if (oldHeaderValue == null) {
        currentSpan.setAttribute(attributeKey, headerValue);
      } else {
        currentSpan.setAttribute(attributeKey, String.join(",", oldHeaderValue, headerValue));
      }
      System.out.println("end Enter javax.servlet.http.HttpServletResponse.addHeader");
    }
  }

  @SuppressWarnings("unused")
  static class HttpServletResponse_setDateHeader {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This HttpServletResponse httpServletResponse,
        @Advice.Argument(value = 0) String headerName,
        @Advice.Argument(value = 1) long headerValue) {
      System.out.printf(
          "start Enter javax.servlet.http.HttpServletResponse.setDateHeader (%s, %d)\n",
          headerName, headerValue);
      Span currentSpan = Java8BytecodeBridge.currentSpan();
      AttributeKey<Long> attributeKey =
          HypertraceSemanticAttributes.httpResponseHeaderLong(headerName);
      currentSpan.setAttribute(attributeKey, headerValue);
      System.out.println("end Enter javax.servlet.http.HttpServletResponse.setDateHeader");
    }
  }

  @SuppressWarnings("unused")
  static class HttpServletResponse_setIntHeader {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This HttpServletResponse httpServletResponse,
        @Advice.Argument(value = 0) String headerName,
        @Advice.Argument(value = 1) int headerValue) {
      System.out.printf(
          "start Enter javax.servlet.http.HttpServletResponse.setIntHeader (%s, %d)\n",
          headerName, headerValue);
      Span currentSpan = Java8BytecodeBridge.currentSpan();
      AttributeKey<Long> attributeKey =
          HypertraceSemanticAttributes.httpResponseHeaderLong(headerName);
      currentSpan.setAttribute(attributeKey, headerValue);
      System.out.println("end Enter javax.servlet.http.HttpServletResponse.setIntHeader");
    }
  }
}
