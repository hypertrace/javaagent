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

package io.opentelemetry.javaagent.instrumentation.hypertrace.java.outputstream;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.extendsClass;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.GlobalObjectRegistry;

/**
 * {@link OutputStream} instrumentation. The type matcher applies to all implementations. However
 * only streams that are in the {@link GlobalObjectRegistry#outputStreamToBufferMap} are
 * instrumented, otherwise the instrumentation is noop.
 *
 * <p>If the stream is in the {@link GlobalObjectRegistry#outputStreamToBufferMap} then arguments to
 * write methods are also passed to the buffered stream (value) from the map. The buffered stream is
 * then used by other instrumentations to capture body.
 */
@AutoService(InstrumentationModule.class)
public class OutputStreamInstrumentationModule extends InstrumentationModule {

  public OutputStreamInstrumentationModule() {
    super("outputstream", "ht");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new OutputStreamInstrumentation());
  }

  static class OutputStreamInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return extendsClass(named(OutputStream.class.getName()));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
      transformers.put(
          named("write")
              .and(takesArguments(1))
              .and(takesArgument(0, is(int.class)))
              .and(isPublic()),
          OutputStreamInstrumentationModule.class.getName() + "$OutputStream_WriteIntAdvice");
      transformers.put(
          named("write")
              .and(takesArguments(1))
              .and(takesArgument(0, is(byte[].class)))
              .and(isPublic()),
          OutputStreamInstrumentationModule.class.getName() + "$OutputStream_WriteByteArrAdvice");
      transformers.put(
          named("write")
              .and(takesArguments(3))
              .and(takesArgument(0, is(byte[].class)))
              .and(takesArgument(1, is(int.class)))
              .and(takesArgument(2, is(int.class)))
              .and(isPublic()),
          OutputStreamInstrumentationModule.class.getName()
              + "$OutputStream_WriteByteArrOffsetAdvice");
      return transformers;
    }
  }

  static class OutputStream_WriteIntAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean enter(@Advice.This OutputStream thizz) {
      return OutputStreamUtils.check(thizz);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This OutputStream thizz,
        @Advice.Argument(0) int b,
        @Advice.Enter boolean retEnterAdvice)
        throws IOException {
      OutputStreamUtils.write(thizz, retEnterAdvice, b);
    }
  }

  static class OutputStream_WriteByteArrAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean enter(@Advice.This OutputStream thizz) {
      return OutputStreamUtils.check(thizz);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This OutputStream thizz,
        @Advice.Argument(0) byte b[],
        @Advice.Enter boolean retEnterAdvice)
        throws IOException {
      OutputStreamUtils.write(thizz, retEnterAdvice, b);
    }
  }

  static class OutputStream_WriteByteArrOffsetAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean enter(@Advice.This OutputStream thizz) {
      return OutputStreamUtils.check(thizz);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This OutputStream thizz,
        @Advice.Argument(0) byte b[],
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len,
        @Advice.Enter boolean retEnterAdvice)
        throws IOException {
      OutputStreamUtils.write(thizz, retEnterAdvice, b, off, len);
    }
  }
}
