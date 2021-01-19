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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.response;

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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletInputStream;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;

public class PrintWriterInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(named("java.io.PrintWriter")).or(named("java.io.PrintWriter"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        named("write")
            .and(takesArguments(1))
            .and(takesArgument(0, is(char[].class)))
            .and(isPublic()),
        PrintWriterInstrumentation.class.getName() + "$Writer_writeArr");
    //    transformers.put(
    //        named("write")
    //            .and(takesArguments(3))
    //            .and(takesArgument(0, is(char[].class)))
    //            .and(takesArgument(1, is(int.class)))
    //            .and(takesArgument(2, is(int.class)))
    //            .and(isPublic()),
    //        PrintWriterInstrumentation.class.getName() + "$Writer_writeOffset");
    return transformers;
  }

  static class Writer_writeArr {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(@Advice.This PrintWriter thizz, @Advice.Argument(0) char[] buf)
        throws IOException {

      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(PrintWriter.class);
      if (callDepth > 0) {
        return;
      }
      BoundedCharArrayWriter buffer =
          InstrumentationContext.get(PrintWriter.class, BoundedCharArrayWriter.class).get(thizz);
      if (buffer != null) {
        buffer.write(buf);
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      CallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
    }
  }
}
