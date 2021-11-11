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

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.tooling.Utils;
import java.io.IOException;
import javax.servlet.ServletInputStream;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.ExceptionHandler;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
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
    transformer.applyTransformer(
        new AgentBuilder.Transformer.ForAdvice()
            .include(
                io.opentelemetry.javaagent.tooling.Utils.getBootstrapProxy(),
                io.opentelemetry.javaagent.tooling.Utils.getAgentClassLoader(),
                Utils.getExtensionsClassLoader())
            .withExceptionHandler(EXCEPTION_STACK_HANDLER)
            .advice(
                named("read").and(takesArguments(0)).and(isPublic()),
                ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadNoArgs"));
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
          InstrumentationContext.get(ServletInputStream.class, ByteBufferSpanPair.class).get(thizz);
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
          bufferSpanPair.buffer.write((byte) read);
        }
      } catch (Throwable t) {
        if (t instanceof HypertraceEvaluationException) {
          throw t;
        } else {
          // TODO find way to log this without mucking with muzzle
        }
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

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
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
      int callDepth =
          HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
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

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
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
      int callDepth =
          HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
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

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
      return bufferSpanPair;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return byte[] b, @Advice.Enter ByteBufferSpanPair bufferSpanPair)
        throws IOException {
      if (bufferSpanPair == null) {
        return;
      }
      int callDepth =
          HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
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

      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletInputStream.class);
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
      int callDepth =
          HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
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

  private static final String HANDLER_NAME =
      "io.opentelemetry.javaagent.bootstrap.ExceptionLogger".replace('.', '/');
  private static final String LOGGER_NAME = "org.slf4j.Logger".replace('.', '/');
  private static final String LOG_FACTORY_NAME = "org.slf4j.LoggerFactory".replace('.', '/');
  private static final ExceptionHandler EXCEPTION_STACK_HANDLER =
      new ExceptionHandler.Simple(
          new StackManipulation() {
            // Pops one Throwable off the stack. Maxes the stack to at least 3.
            private final StackManipulation.Size size = new StackManipulation.Size(-1, 3);

            @Override
            public boolean isValid() {
              return true;
            }

            @Override
            public StackManipulation.Size apply(MethodVisitor mv, Implementation.Context context) {
              String name = context.getInstrumentedType().getName();
              ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

              // writes the following bytecode:
              // try {
              //   org.slf4j.LoggerFactory.getLogger((Class)ExceptionLogger.class)
              //     .debug("exception in instrumentation", t);
              // } catch (Throwable t2) {
              // }
              Label logStart = new Label();
              Label logEnd = new Label();
              Label eatException = new Label();
              Label handlerExit = new Label();

              // Frames are only meaningful for class files in version 6 or later.
              boolean frames = context.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6);

              mv.visitTryCatchBlock(logStart, logEnd, eatException, "java/lang/Throwable");

              // stack: (top) throwable
              mv.visitLabel(logStart);
              mv.visitLdcInsn(Type.getType("L" + HANDLER_NAME + ";"));
              mv.visitMethodInsn(
                  Opcodes.INVOKESTATIC,
                  LOG_FACTORY_NAME,
                  "getLogger",
                  "(Ljava/lang/Class;)L" + LOGGER_NAME + ";",
                  /* isInterface= */ false);
              mv.visitInsn(Opcodes.SWAP); // stack: (top) throwable,logger
              mv.visitLdcInsn(
                  "Failed to handle exception in instrumentation for "
                      + name
                      + " on "
                      + classLoader);
              mv.visitInsn(Opcodes.SWAP); // stack: (top) throwable,string,logger
              mv.visitMethodInsn(
                  Opcodes.INVOKEINTERFACE,
                  LOGGER_NAME,
                  "debug",
                  "(Ljava/lang/String;Ljava/lang/Throwable;)V",
                  /* isInterface= */ true);
              mv.visitLabel(logEnd);
              mv.visitJumpInsn(Opcodes.GOTO, handlerExit);

              // if the runtime can't reach our ExceptionHandler or logger,
              //   silently eat the exception
              mv.visitLabel(eatException);
              if (frames) {
                mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
              }
              mv.visitInsn(Opcodes.POP);
              // mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable",
              //    "printStackTrace", "()V", false);

              mv.visitLabel(handlerExit);
              if (frames) {
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
              }

              return size;
            }
          });
}
