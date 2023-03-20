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
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.instrumentation.api.field.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.io.IOException;
import javax.servlet.ServletOutputStream;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.HypertraceCallDepthThreadLocalMap;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;

public class ServletOutputStreamInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("javax.servlet.ServletOutputStream"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("print")
            .and(takesArguments(1))
            .and(takesArgument(0, is(String.class)))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$ServletOutputStream_print");
    // other print methods call print or write on the OutputStream

    // OutputStream methods
    transformer.applyAdviceToMethod(
        named("write").and(takesArguments(1)).and(takesArgument(0, is(int.class))).and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$OutputStream_write");
    transformer.applyAdviceToMethod(
        named("write")
            .and(takesArguments(1))
            .and(takesArgument(0, is(byte[].class)))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$OutputStream_writeByteArr");
    transformer.applyAdviceToMethod(
        named("write")
            .and(takesArguments(3))
            .and(takesArgument(0, is(byte[].class)))
            .and(takesArgument(1, is(int.class)))
            .and(takesArgument(2, is(int.class)))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$OutputStream_writeByteArrOffset");

    // close() is not instrumented due to some issue with Tomcat
    // refer the comment in servlet-3.0 code
  }

  @SuppressWarnings("unused")
  static class OutputStream_write {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static BoundedByteArrayOutputStream enter(
        @Advice.This ServletOutputStream thizz, @Advice.Argument(0) int b) {
      BoundedByteArrayOutputStream buffer =
          VirtualField.find(ServletOutputStream.class, BoundedByteArrayOutputStream.class)
              .get(thizz);
      if (buffer == null) {
        return null;
      }
      int callDepth =
          HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletOutputStream.class);
      if (callDepth > 0) {
        return buffer;
      }

      buffer.write(b);
      return buffer;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.Enter BoundedByteArrayOutputStream buffer) {
      if (buffer != null) {
        HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletOutputStream.class);
      }
    }
  }

  @SuppressWarnings("unused")
  static class OutputStream_writeByteArr {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static BoundedByteArrayOutputStream enter(
        @Advice.This ServletOutputStream thizz, @Advice.Argument(0) byte[] b) throws IOException {

      BoundedByteArrayOutputStream buffer =
          VirtualField.find(ServletOutputStream.class, BoundedByteArrayOutputStream.class)
              .get(thizz);
      if (buffer == null) {
        return null;
      }
      int callDepth =
          HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletOutputStream.class);
      if (callDepth > 0) {
        return buffer;
      }

      buffer.write(b);
      return buffer;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.Enter BoundedByteArrayOutputStream buffer) {
      if (buffer != null) {
        HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletOutputStream.class);
      }
    }
  }

  @SuppressWarnings("unused")
  static class OutputStream_writeByteArrOffset {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static BoundedByteArrayOutputStream enter(
        @Advice.This ServletOutputStream thizz,
        @Advice.Argument(0) byte[] b,
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len) {

      BoundedByteArrayOutputStream buffer =
          VirtualField.find(ServletOutputStream.class, BoundedByteArrayOutputStream.class)
              .get(thizz);
      if (buffer == null) {
        return null;
      }
      int callDepth =
          HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletOutputStream.class);
      if (callDepth > 0) {
        return buffer;
      }

      buffer.write(b, off, len);
      return buffer;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.Enter BoundedByteArrayOutputStream buffer) {
      if (buffer != null) {
        HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletOutputStream.class);
      }
    }
  }

  @SuppressWarnings("unused")
  static class ServletOutputStream_print {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static BoundedByteArrayOutputStream enter(
        @Advice.This ServletOutputStream thizz, @Advice.Argument(0) String s) throws IOException {

      BoundedByteArrayOutputStream buffer =
          VirtualField.find(ServletOutputStream.class, BoundedByteArrayOutputStream.class)
              .get(thizz);
      if (buffer == null) {
        return null;
      }
      int callDepth =
          HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletOutputStream.class);
      if (callDepth > 0) {
        return buffer;
      }

      String bodyPart = s == null ? "null" : s;
      buffer.write(bodyPart.getBytes());
      return buffer;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.Enter BoundedByteArrayOutputStream buffer) {
      if (buffer != null) {
        HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletOutputStream.class);
      }
    }
  }
}
