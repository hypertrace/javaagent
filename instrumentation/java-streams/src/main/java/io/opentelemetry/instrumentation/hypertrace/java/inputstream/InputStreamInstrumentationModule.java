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

package io.opentelemetry.instrumentation.hypertrace.java.inputstream;

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
import org.hypertrace.agent.core.GlobalObjectRegistry;

/**
 * {@link InputStream} instrumentation. The type matcher applies to all implementations. However
 * only streams that are in the {@link GlobalObjectRegistry#inputStreamToSpanAndBufferMap} are
 * instrumented, otherwise the instrumentation is noop.
 *
 * <p>If the stream is in the {@link GlobalObjectRegistry#inputStreamToSpanAndBufferMap} then result
 * of read methods is also passed to the buffered stream (value) from the map. The instrumentation
 * adds buffer to span from the map when read is finished (return -1), creates new span with buffer
 * when the original span is not recording.
 *
 * <p>Maybe we could add optimization to instrument the input streams only when certain classes are
 * present in classloader e.g. classes from frameworks that we instrument.
 */
@AutoService(InstrumentationModule.class)
public class InputStreamInstrumentationModule extends InstrumentationModule {

  public InputStreamInstrumentationModule() {
    super("inputstream");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new InputStreamInstrumentation());
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
          InputStreamInstrumentationModule.class.getName() + "$InputStream_ReadNoArgsAdvice");
      transformers.put(
          namedOneOf("read")
              .and(takesArguments(1))
              .and(takesArgument(0, is(byte[].class)))
              .and(isPublic()),
          InputStreamInstrumentationModule.class.getName() + "$InputStream_ReadByteArrayAdvice");
      transformers.put(
          namedOneOf("read")
              .and(takesArguments(3))
              .and(takesArgument(0, is(byte[].class)))
              .and(takesArgument(1, is(int.class)))
              .and(takesArgument(2, is(int.class)))
              .and(isPublic()),
          InputStreamInstrumentationModule.class.getName()
              + "$InputStream_ReadByteArrayOffsetAdvice");
      transformers.put(
          namedOneOf("readAllBytes").and(takesArguments(0)).and(isPublic()),
          InputStream_ReadAllBytes.class.getName());
      transformers.put(
          namedOneOf("readNBytes")
              .and(takesArguments(0))
              .and(takesArgument(0, is(byte[].class)))
              .and(takesArgument(1, is(int.class)))
              .and(takesArgument(2, is(int.class)))
              .and(isPublic()),
          InputStreamInstrumentationModule.class.getName() + "$InputStream_ReadNBytes");
      return transformers;
    }
  }

  public static class InputStream_ReadNoArgsAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.This InputStream thizz, @Advice.Return int read) {
      InputStreamUtils.read(thizz, read);
    }
  }

  public static class InputStream_ReadByteArrayAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.This InputStream thizz, @Advice.Return int read, @Advice.Argument(0) byte b[]) {
      InputStreamUtils.read(thizz, read, b);
    }
  }

  public static class InputStream_ReadByteArrayOffsetAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.This InputStream thizz,
        @Advice.Return int read,
        @Advice.Argument(0) byte b[],
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len) {
      InputStreamUtils.read(thizz, read, b, off, len);
    }
  }

  public static class InputStream_ReadAllBytes {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.This InputStream thizz, @Advice.Return byte[] b)
        throws IOException {
      InputStreamUtils.readAll(thizz, b);
    }
  }

  public static class InputStream_ReadNBytes {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.This InputStream thizz,
        @Advice.Return int read,
        @Advice.Argument(0) byte[] b,
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len)
        throws IOException {
      InputStreamUtils.readNBytes(thizz, read, b, off, len);
    }
  }
}
