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
import javax.servlet.ServletInputStream;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.ByteBufferSpanPair;

public class ServletInputStreamInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(named("javax.servlet.ServletInputStream"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        named("read").and(takesArguments(0)).and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadNoArgs");
    transformers.put(
        named("read")
            .and(takesArguments(1))
            .and(takesArgument(0, is(byte[].class)))
            .and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArray");
    transformers.put(
        named("read")
            .and(takesArguments(3))
            .and(takesArgument(0, is(byte[].class)))
            .and(takesArgument(1, is(int.class)))
            .and(takesArgument(2, is(int.class)))
            .and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArrayOffset");
    transformers.put(
        named("readAllBytes").and(takesArguments(0)).and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadAllBytes");
    transformers.put(
        named("readNBytes")
            .and(takesArguments(0))
            .and(takesArgument(0, is(byte[].class)))
            .and(takesArgument(1, is(int.class)))
            .and(takesArgument(2, is(int.class)))
            .and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadNBytes");

    // ServletInputStream methods
    transformers.put(
        named("readLine")
            .and(takesArguments(3))
            .and(takesArgument(0, is(byte[].class)))
            .and(takesArgument(1, is(int.class)))
            .and(takesArgument(2, is(int.class)))
            .and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArrayOffset");
    return transformers;
  }

  static class InputStream_ReadNoArgs {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      ByteBufferSpanPair bufferSpanPair =
          InstrumentationContext.get(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        return null;
      }

      CallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return int read, @Advice.Enter ByteBufferSpanPair bufferSpanPair) {
      if (bufferSpanPair == null) {
        return;
      }
      int callDepth = CallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
      if (callDepth > 0) {
        return;
      }

      if (read == -1) {
        bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
      } else {
        bufferSpanPair.buffer.write((byte) read);
      }
    }
  }

  public static class InputStream_ReadByteArray {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      ByteBufferSpanPair bufferSpanPair =
          InstrumentationContext.get(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        return null;
      }

      CallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return int read,
        @Advice.Argument(0) byte b[],
        @Advice.Enter ByteBufferSpanPair bufferSpanPair) {
      if (bufferSpanPair == null) {
        return;
      }
      int callDepth = CallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
      if (callDepth > 0) {
        return;
      }

      if (read == -1) {
        bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
      } else {
        bufferSpanPair.buffer.write(b, 0, read);
      }
    }
  }

  public static class InputStream_ReadByteArrayOffset {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      ByteBufferSpanPair bufferSpanPair =
          InstrumentationContext.get(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        return null;
      }

      CallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return int read,
        @Advice.Argument(0) byte b[],
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len,
        @Advice.Enter ByteBufferSpanPair bufferSpanPair) {
      if (bufferSpanPair == null) {
        return;
      }
      int callDepth = CallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
      if (callDepth > 0) {
        return;
      }

      if (read == -1) {
        bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
      } else {
        bufferSpanPair.buffer.write(b, off, read);
      }
    }
  }

  public static class InputStream_ReadAllBytes {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      ByteBufferSpanPair bufferSpanPair =
          InstrumentationContext.get(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        return null;
      }

      CallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return byte[] b, @Advice.Enter ByteBufferSpanPair bufferSpanPair)
        throws IOException {
      if (bufferSpanPair == null) {
        return;
      }
      int callDepth = CallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
      if (callDepth > 0) {
        return;
      }

      bufferSpanPair.buffer.write(b);
      bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
    }
  }

  public static class InputStream_ReadNBytes {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      ByteBufferSpanPair bufferSpanPair =
          InstrumentationContext.get(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        return null;
      }

      CallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return int read,
        @Advice.Argument(0) byte[] b,
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len,
        @Advice.Enter ByteBufferSpanPair bufferSpanPair) {
      if (bufferSpanPair == null) {
        return;
      }
      int callDepth = CallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
      if (callDepth > 0) {
        return;
      }

      if (read == -1) {
        bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
      } else {
        bufferSpanPair.buffer.write(b, off, read);
      }
    }
  }
}
