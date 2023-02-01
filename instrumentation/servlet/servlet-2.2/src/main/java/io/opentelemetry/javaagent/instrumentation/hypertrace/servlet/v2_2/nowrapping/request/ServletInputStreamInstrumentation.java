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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v2_2.nowrapping.request;

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
    // TODO: readNBytes(int len) is not transformed
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

  @SuppressWarnings("unused")
  static class InputStream_ReadNoArgs {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      System.out.println("start Enter javax.servlet.ServletInputStream.ReadNoArgs");
      ByteBufferSpanPair bufferSpanPair =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        System.out.println("end1 Enter javax.servlet.ServletInputStream.ReadNoArgs");
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      System.out.println("end Enter javax.servlet.ServletInputStream.ReadNoArgs");
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return int read, @Advice.Enter ByteBufferSpanPair bufferSpanPair) {
      System.out.println("start Exit javax.servlet.ServletInputStream.ReadNoArgs");
      try {
        if (bufferSpanPair == null) {
          System.out.println("end1 Exit javax.servlet.ServletInputStream.ReadNoArgs");
          return;
        }
        int callDepth =
            HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
        if (callDepth > 0) {
          System.out.println("end2 Exit javax.servlet.ServletInputStream.ReadNoArgs");
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
      System.out.println("end Exit javax.servlet.ServletInputStream.ReadNoArgs");
    }
  }

  @SuppressWarnings("unused")
  public static class InputStream_ReadByteArray {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      System.out.println("start Enter javax.servlet.ServletInputStream.ReadByteArray");
      ByteBufferSpanPair bufferSpanPair =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        System.out.println("end1 Enter javax.servlet.ServletInputStream.ReadByteArray");
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      System.out.println("end Enter javax.servlet.ServletInputStream.ReadByteArray");
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletInputStream thizz,
        @Advice.Return int read,
        @Advice.Argument(0) byte[] b,
        @Advice.Enter ByteBufferSpanPair bufferSpanPair)
        throws Throwable {
      System.out.println("start Exit javax.servlet.ServletInputStream.ReadByteArray");
      try {
        if (bufferSpanPair == null) {
          System.out.println("end1 Exit javax.servlet.ServletInputStream.ReadByteArray");
          return;
        }
        int callDepth =
            HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
        if (callDepth > 0) {
          System.out.println("end2 Exit javax.servlet.ServletInputStream.ReadByteArray");
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
      System.out.println("end Exit javax.servlet.ServletInputStream.ReadByteArray");
    }
  }

  @SuppressWarnings("unused")
  public static class InputStream_ReadByteArrayOffset {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      System.out.println("start Enter javax.servlet.ServletInputStream.ReadByteArrayOffset");
      ByteBufferSpanPair bufferSpanPair =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        System.out.println("end1 Enter javax.servlet.ServletInputStream.ReadByteArrayOffset");
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      System.out.println("end Enter javax.servlet.ServletInputStream.ReadByteArrayOffset");
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
      System.out.println("start Exit javax.servlet.ServletInputStream.ReadByteArrayOffset");
      try {

        if (bufferSpanPair == null) {
          System.out.println("end1 Exit javax.servlet.ServletInputStream.ReadByteArrayOffset");
          return;
        }
        int callDepth =
            HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
        if (callDepth > 0) {
          System.out.println("end2 Exit javax.servlet.ServletInputStream.ReadByteArrayOffset");
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
      System.out.println("end Exit javax.servlet.ServletInputStream.ReadByteArrayOffset");
    }
  }

  @SuppressWarnings("unused")
  public static class InputStream_ReadAllBytes {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      System.out.println("start Enter javax.servlet.ServletInputStream.ReadAllBytes");
      ByteBufferSpanPair bufferSpanPair =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        System.out.println("end1 Enter javax.servlet.ServletInputStream.ReadAllBytes");
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      System.out.println("end Enter javax.servlet.ServletInputStream.ReadAllBytes");
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return byte[] b, @Advice.Enter ByteBufferSpanPair bufferSpanPair)
        throws IOException {
      System.out.println("start Exit javax.servlet.ServletInputStream.ReadAllBytes");
      try {
        if (bufferSpanPair == null) {
          System.out.println("end1 Exit javax.servlet.ServletInputStream.ReadAllBytes");
          return;
        }
        int callDepth =
            HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
        if (callDepth > 0) {
          System.out.println("end2 Exit javax.servlet.ServletInputStream.ReadAllBytes");
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
      System.out.println("end Exit javax.servlet.ServletInputStream.ReadAllBytes");
    }
  }

  @SuppressWarnings("unused")
  public static class InputStream_ReadNBytes {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ByteBufferSpanPair enter(@Advice.This ServletInputStream thizz) {
      System.out.println("start Enter javax.servlet.ServletInputStream.ReadNBytes");
      ByteBufferSpanPair bufferSpanPair =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
      if (bufferSpanPair == null) {
        System.out.println("end1 Enter javax.servlet.ServletInputStream.ReadNBytes");
        return null;
      }

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      System.out.println("end Enter javax.servlet.ServletInputStream.ReadNBytes");
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
        @Advice.This ServletInputStream thizz,
        @Advice.Return int read,
        @Advice.Argument(0) byte[] b,
        @Advice.Argument(1) int off,
        @Advice.Enter ByteBufferSpanPair bufferSpanPair)
        throws Throwable {
      System.out.println("start Exit javax.servlet.ServletInputStream.ReadNBytes");
      try {
        if (bufferSpanPair == null) {
          System.out.println("end1 Exit javax.servlet.ServletInputStream.ReadNBytes");
          return;
        }
        int callDepth =
            HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
        if (callDepth > 0) {
          System.out.println("end2 Exit javax.servlet.ServletInputStream.ReadNBytes");
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
      System.out.println("end Exit javax.servlet.ServletInputStream.ReadNBytes");
    }
  }
}
