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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.response;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;

public class ServletOutputStreamInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(named("javax.servlet.ServletOutputStream"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        named("print")
            .and(takesArguments(1))
            .and(takesArgument(0, is(String.class)))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$ServletOutputStream_print");
    // other print methods call print or write on the OutputStream

    // OutputStream methods
    transformers.put(
        named("write").and(takesArguments(1)).and(takesArgument(0, is(int.class))).and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$OutputStream_write");
    transformers.put(
        named("write")
            .and(takesArguments(1))
            .and(takesArgument(0, is(byte[].class)))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$OutputStream_writeByteArr");
    transformers.put(
        named("write")
            .and(takesArguments(3))
            .and(takesArgument(0, is(byte[].class)))
            .and(takesArgument(1, is(int.class)))
            .and(takesArgument(2, is(int.class)))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$OutputStream_writeByteArrOffset");

    // close is not called on Tomcat (tested with Spring Boot)
    //    transformers.put(
    //        named("close").and(takesArguments(0))
    //            .and(isPublic()),
    //        ServletOutputStreamInstrumentation.class.getName() + "$OutputStream_close");
    return transformers;
  }

  static class OutputStream_write {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(@Advice.This ServletOutputStream thizz, @Advice.Argument(0) int b) {
      System.out.println("write");
      System.out.println(String.valueOf(b));
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(ServletOutputStream.class);
      if (callDepth > 0) {
        return;
      }
      BoundedByteArrayOutputStream buffer =
          InstrumentationContext.get(ServletOutputStream.class, BoundedByteArrayOutputStream.class)
              .get(thizz);
      if (buffer != null) {
        buffer.write(b);
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      CallDepthThreadLocalMap.decrementCallDepth(ServletOutputStream.class);
    }
  }

  static class OutputStream_writeByteArr {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(@Advice.This ServletOutputStream thizz, @Advice.Argument(0) byte[] b)
        throws IOException {
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(ServletOutputStream.class);
      if (callDepth > 0) {
        return;
      }
      BoundedByteArrayOutputStream buffer =
          InstrumentationContext.get(ServletOutputStream.class, BoundedByteArrayOutputStream.class)
              .get(thizz);
      if (buffer != null) {
        buffer.write(b);
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      CallDepthThreadLocalMap.decrementCallDepth(ServletOutputStream.class);
    }
  }

  static class OutputStream_writeByteArrOffset {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This ServletOutputStream thizz,
        @Advice.Argument(0) byte b[],
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len) {
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(ServletOutputStream.class);
      if (callDepth > 0) {
        return;
      }
      BoundedByteArrayOutputStream buffer =
          InstrumentationContext.get(ServletOutputStream.class, BoundedByteArrayOutputStream.class)
              .get(thizz);
      if (buffer != null) {
        buffer.write(b, off, len);
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      CallDepthThreadLocalMap.decrementCallDepth(ServletOutputStream.class);
    }
  }

  static class ServletOutputStream_print {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(@Advice.This ServletOutputStream thizz, @Advice.Argument(0) String s)
        throws IOException {
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(ServletOutputStream.class);
      if (callDepth > 0) {
        return;
      }
      BoundedByteArrayOutputStream buffer =
          InstrumentationContext.get(ServletOutputStream.class, BoundedByteArrayOutputStream.class)
              .get(thizz);
      if (buffer != null) {
        String bodyPart = s == null ? "null" : s;
        buffer.write(bodyPart.getBytes());
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      CallDepthThreadLocalMap.decrementCallDepth(ServletOutputStream.class);
    }
  }
}
