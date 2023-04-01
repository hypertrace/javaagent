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

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.io.IOException;
import javax.servlet.ServletInputStream;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.HypertraceCallDepthThreadLocalMap;
import org.hypertrace.agent.core.instrumentation.HypertraceEvaluationException;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.ByteBufferSpanPair;

public class ServletInputStreamInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("javax.servlet.ServletInputStream"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("read").and(takesArguments(0)).and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadNoArgs");
    transformer.applyAdviceToMethod(
        named("read")
            .and(takesArguments(1))
            .and(takesArgument(0, is(byte[].class)))
            .and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArray");
    transformer.applyAdviceToMethod(
        named("read")
            .and(takesArguments(3))
            .and(takesArgument(0, is(byte[].class)))
            .and(takesArgument(1, is(int.class)))
            .and(takesArgument(2, is(int.class)))
            .and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArrayOffset");
    transformer.applyAdviceToMethod(
        named("readAllBytes").and(takesArguments(0)).and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadAllBytes");
    transformer.applyAdviceToMethod(
        named("readNBytes")
            .and(takesArguments(0))
            .and(takesArgument(0, is(byte[].class)))
            .and(takesArgument(1, is(int.class)))
            .and(takesArgument(2, is(int.class)))
            .and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadNBytes");

    // ServletInputStream methods
    transformer.applyAdviceToMethod(
        named("readLine")
            .and(takesArguments(3))
            .and(takesArgument(0, is(byte[].class)))
            .and(takesArgument(1, is(int.class)))
            .and(takesArgument(2, is(int.class)))
            .and(isPublic()),
        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArrayOffset");
  }

  static class InputStream_ReadNoArgs {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      ByteBufferSpanPair bufferSpanPair =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return int read, @Advice.Enter ByteBufferSpanPair bufferSpanPair) {
      try {
        if (bufferSpanPair == null) {
          return;
        }
        int callDepth =
            HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
        if (callDepth > 0) {
          return;
        }

        if (read == -1) {
          bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
        } else {
          bufferSpanPair.writeToBuffer((byte) read);
        }
      } catch (Throwable t) {
        if (t instanceof HypertraceEvaluationException) {
          throw t;
        } else {
          // ignore
        }
      }
    }
  }

  public static class InputStream_ReadByteArray {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      ByteBufferSpanPair bufferSpanPair =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletInputStream thizz,
        @Advice.Return int read,
        @Advice.Argument(0) byte b[],
        @Advice.Enter ByteBufferSpanPair bufferSpanPair)
        throws Throwable {
      try {
        if (bufferSpanPair == null) {
          return;
        }
        int callDepth =
            HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
        if (callDepth > 0) {
          return;
        }

        if (read == -1) {
          bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
        } else {
          bufferSpanPair.writeToBuffer(b, 0, read);
          if (thizz.available() == 0) {
            bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
          }
        }
      } catch (Throwable t) {
        if (t instanceof HypertraceEvaluationException) {
          throw t;
        } else {
          // ignore
        }
      }
    }
  }

  public static class InputStream_ReadByteArrayOffset {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      ByteBufferSpanPair bufferSpanPair =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletInputStream thizz,
        @Advice.Return int read,
        @Advice.Argument(0) byte b[],
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len,
        @Advice.Enter ByteBufferSpanPair bufferSpanPair)
        throws Throwable {
      try {

        if (bufferSpanPair == null) {
          return;
        }
        int callDepth =
            HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
        if (callDepth > 0) {
          return;
        }

        if (read == -1) {
          bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
        } else {
          bufferSpanPair.writeToBuffer(b, off, read);
          if (thizz.available() == 0) {
            bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
          }
        }
      } catch (Throwable t) {
        if (t instanceof HypertraceEvaluationException) {
          throw t;
        } else {
          // ignore
        }
      }
    }
  }

  public static class InputStream_ReadAllBytes {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      ByteBufferSpanPair bufferSpanPair =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return byte[] b, @Advice.Enter ByteBufferSpanPair bufferSpanPair)
        throws IOException {
      try {
        if (bufferSpanPair == null) {
          return;
        }
        int callDepth =
            HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
        if (callDepth > 0) {
          return;
        }
        bufferSpanPair.writeToBuffer(b);
        bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
      } catch (Throwable t) {
        if (t instanceof HypertraceEvaluationException) {
          throw t;
        } else {
          // ignore
        }
      }
    }
  }

  public static class InputStream_ReadNBytes {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      ByteBufferSpanPair bufferSpanPair =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletInputStream thizz,
        @Advice.Return int read,
        @Advice.Argument(0) byte[] b,
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len,
        @Advice.Enter ByteBufferSpanPair bufferSpanPair)
        throws Throwable {
      try {
        if (bufferSpanPair == null) {
          return;
        }
        int callDepth =
            HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
        if (callDepth > 0) {
          return;
        }

        if (read == -1) {
          bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
        } else {
          bufferSpanPair.writeToBuffer(b, off, read);
          if (thizz.available() == 0) {
            bufferSpanPair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);
          }
        }
      } catch (Throwable t) {
        if (t instanceof HypertraceEvaluationException) {
          throw t;
        } else {
          // ignore
        }
      }
    }
  }

  private static final String HANDLER_NAME =
      "io.opentelemetry.javaagent.bootstrap.ExceptionLogger".replace('.', '/');
  private static final String LOGGER_NAME = "org.slf4j.Logger".replace('.', '/');
  private static final String LOG_FACTORY_NAME = "org.slf4j.LoggerFactory".replace('.', '/');
}
