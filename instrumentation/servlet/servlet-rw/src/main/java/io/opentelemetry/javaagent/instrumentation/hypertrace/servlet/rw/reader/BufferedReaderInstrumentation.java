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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.rw.reader;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.CharBufferSpanPair;

public class BufferedReaderInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(named("java.io.BufferedReader")).or(named("java.io.BufferedReader"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        named("read").and(takesArguments(0)).and(isPublic()),
        BufferedReaderInstrumentation.class.getName() + "$Reader_readNoArgs");
    transformers.put(
        named("read")
            .and(takesArguments(1))
            .and(takesArgument(0, is(char[].class)))
            .and(isPublic()),
        BufferedReaderInstrumentation.class.getName() + "$Reader_readCharArray");
    transformers.put(
        named("read")
            .and(takesArguments(3))
            .and(takesArgument(0, is(char[].class)))
            .and(takesArgument(1, is(int.class)))
            .and(takesArgument(2, is(int.class)))
            .and(isPublic()),
        BufferedReaderInstrumentation.class.getName() + "$Reader_readByteArrayOffset");
    transformers.put(
        named("readLine").and(takesArguments(0)).and(isPublic()),
        BufferedReaderInstrumentation.class.getName() + "$BufferedReader_readLine");
    return transformers;
  }

  static class Reader_readNoArgs {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static CharBufferSpanPair enter(@Advice.This BufferedReader thizz) {
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(BufferedReader.class);
      if (callDepth > 0) {
        return null;
      }
      return InstrumentationContext.get(BufferedReader.class, CharBufferSpanPair.class).get(thizz);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This BufferedReader thizz,
        @Advice.Return int read,
        @Advice.Enter CharBufferSpanPair bufferSpanPair) {
      CallDepthThreadLocalMap.decrementCallDepth(BufferedReader.class);
      if (bufferSpanPair == null) {
        return;
      }
      if (read == -1) {
        bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
      } else {
        bufferSpanPair.buffer.write(read);
      }
      CallDepthThreadLocalMap.reset(BufferedReader.class);
    }
  }

  static class Reader_readCharArray {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static CharBufferSpanPair enter(@Advice.This BufferedReader thizz) {
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(BufferedReader.class);
      if (callDepth > 0) {
        return null;
      }
      return InstrumentationContext.get(BufferedReader.class, CharBufferSpanPair.class).get(thizz);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return int read,
        @Advice.Argument(0) char c[],
        @Advice.Enter CharBufferSpanPair bufferSpanPair) {
      CallDepthThreadLocalMap.decrementCallDepth(BufferedReader.class);
      if (bufferSpanPair == null) {
        return;
      }
      if (read == -1) {
        bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
      } else {
        bufferSpanPair.buffer.write(c, 0, read);
      }
      CallDepthThreadLocalMap.reset(BufferedReader.class);
    }
  }

  static class Reader_readByteArrayOffset {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static CharBufferSpanPair enter(@Advice.This BufferedReader thizz) {
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(BufferedReader.class);
      if (callDepth > 0) {
        return null;
      }
      return InstrumentationContext.get(BufferedReader.class, CharBufferSpanPair.class).get(thizz);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return int read,
        @Advice.Argument(0) char c[],
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len,
        @Advice.Enter CharBufferSpanPair bufferSpanPair) {
      CallDepthThreadLocalMap.decrementCallDepth(BufferedReader.class);
      if (bufferSpanPair == null) {
        return;
      }
      if (read == -1) {
        bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
      } else {
        bufferSpanPair.buffer.write(c, off, read);
      }
      CallDepthThreadLocalMap.reset(BufferedReader.class);
    }
  }

  static class BufferedReader_readLine {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static CharBufferSpanPair enter(@Advice.This BufferedReader thizz) {
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(BufferedReader.class);
      if (callDepth > 0) {
        return null;
      }
      return InstrumentationContext.get(BufferedReader.class, CharBufferSpanPair.class).get(thizz);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return String line, @Advice.Enter CharBufferSpanPair bufferSpanPair)
        throws IOException {
      CallDepthThreadLocalMap.decrementCallDepth(BufferedReader.class);
      if (bufferSpanPair == null) {
        return;
      }
      if (line == null) {
        bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
      } else {
        bufferSpanPair.buffer.write(line);
      }
      CallDepthThreadLocalMap.reset(BufferedReader.class);
    }
  }
}
