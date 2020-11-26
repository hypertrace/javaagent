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

package io.opentelemetry.instrumentation.hypertrace.apache.httpclient.readall;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static io.opentelemetry.javaagent.tooling.matcher.NameMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.GlobalContextHolder;

@AutoService(InstrumentationModule.class)
public class InputStreamReadAllInstrumentationModule extends InstrumentationModule {

  public InputStreamReadAllInstrumentationModule() {
    super("inputstream-readall");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(
        new InputStreamReadAllInstrumentationModule.InputStreamInstrumentation());
  }

  static class InputStreamInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return safeHasSuperType(namedOneOf("java.io.InputStream"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
      transformers.put(
          namedOneOf("read").and(takesArguments(0)).and(isPublic()),
          InputStreamReadAllInstrumentationModule.InputStream_ReadNoArgsAdvice.class.getName());
      transformers.put(
          namedOneOf("read")
              .and(takesArguments(1))
              .and(takesArgument(0, is(byte[].class)))
              .and(isPublic()),
          InputStreamReadAllInstrumentationModule.InputStream_ReadByteArrayAdvice.class.getName());
      transformers.put(
          namedOneOf("read")
              .and(takesArguments(3))
              .and(takesArgument(0, is(byte[].class)))
              .and(takesArgument(1, is(int.class)))
              .and(takesArgument(2, is(int.class)))
              .and(isPublic()),
          InputStreamReadAllInstrumentationModule.InputStream_ReadByteArrayOffsetAdvice.class
              .getName());
      // TODO JDK9 defines #readAllBytes method
      // https://docs.oracle.com/javase/9/docs/api/java/io/InputStream.html#readAllBytes--
      return transformers;
    }
  }

  /**
   * 1. the instrumentation checks if stream is in the global map. The global map contains map of
   * original stream to buffered one. 2. if it is then body of the original method is skipped, if it
   * is not it continues 3. the exit advice changes return value to buffered stream.
   */
  public static class InputStream_ReadNoArgsAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class, skipOn = Advice.OnNonDefaultValue.class)
    public static boolean readStart(@Advice.This InputStream thizz) {
      InputStream bufferedInputStream = GlobalContextHolder.inputStreamMap.get(thizz);
      if (bufferedInputStream == null) {
        return false;
      }
      return true;
    }

    // not suppressing exceptions since we emulate the
    @Advice.OnMethodExit()
    public static void readEnd(
        @Advice.This java.io.InputStream thizz, @Advice.Return(readOnly = false) int read)
        throws IOException {
      InputStream bufferedInputStream = GlobalContextHolder.inputStreamMap.get(thizz);
      if (bufferedInputStream == null) {
        return;
      }
      read = bufferedInputStream.read();
    }
  }

  public static class InputStream_ReadByteArrayAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class, skipOn = Advice.OnNonDefaultValue.class)
    public static boolean readStart(@Advice.This InputStream thizz) {
      InputStream bufferedInputStream = GlobalContextHolder.inputStreamMap.get(thizz);
      if (bufferedInputStream == null) {
        return false;
      }
      return true;
    }

    @Advice.OnMethodExit()
    public static void readEnd(
        @Advice.This java.io.InputStream thizz,
        @Advice.Return(readOnly = false) int read,
        @Advice.Argument(value = 0, readOnly = false) byte b[])
        throws IOException {
      InputStream bufferedInputStream = GlobalContextHolder.inputStreamMap.get(thizz);
      if (bufferedInputStream == null) {
        return;
      }
      read = bufferedInputStream.read(b);
    }
  }

  public static class InputStream_ReadByteArrayOffsetAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class, skipOn = Advice.OnNonDefaultValue.class)
    public static boolean readStart(@Advice.This InputStream thizz) {
      InputStream bufferedInputStream = GlobalContextHolder.inputStreamMap.get(thizz);
      if (bufferedInputStream == null) {
        return false;
      }
      return true;
    }

    @Advice.OnMethodExit()
    public static void readEnd(
        @Advice.This java.io.InputStream thizz,
        @Advice.Return(readOnly = false) int read,
        @Advice.Argument(value = 0, readOnly = false) byte b[],
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len)
        throws IOException {
      InputStream bufferedInputStream = GlobalContextHolder.inputStreamMap.get(thizz);
      if (bufferedInputStream == null) {
        return;
      }
      read = bufferedInputStream.read(b, off, len);
    }
  }
}
