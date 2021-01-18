package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.response;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.Metadata;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;

public class ServletOutputStreamInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(named("javax.servlet.ServletOutputStream"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        named("print").and(takesArguments(1))
            .and(takesArgument(0, is(String.class)))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$ServletOutputStream_print");
    transformers.put(
        named("write").and(takesArguments(1))
            .and(takesArgument(0, is(int.class)))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$OutputStream_write");
    transformers.put(
        named("write")
            .and(takesArguments(1))
            .and(takesArgument(0, is(byte[].class)))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$OutputStream_writeByteArr");
    transformers.put(
        named("write")
            .and(takesArguments(3))
            .and(takesArgument(0, is(byte[].class)))
            .and(takesArgument(1, is(int.class)))
            .and(takesArgument(2, is(int.class)))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName()
            + "$OutputStream_writeByteArrOffset");
    transformers.put(
        named("close").and(takesArguments(0))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$OutputStream_close");
    transformers.put(
        named("flush").and(takesArguments(0))
            .and(isPublic()),
        ServletOutputStreamInstrumentation.class.getName() + "$OutputStream_flush");
//    transformers.put(
//        named("read")
//            .and(takesArguments(1))
//            .and(takesArgument(0, is(byte[].class)))
//            .and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArray");
//    transformers.put(
//        named("read")
//            .and(takesArguments(3))
//            .and(takesArgument(0, is(byte[].class)))
//            .and(takesArgument(1, is(int.class)))
//            .and(takesArgument(2, is(int.class)))
//            .and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArrayOffset");
//    transformers.put(
//        named("readAllBytes").and(takesArguments(0)).and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadAllBytes");
//    transformers.put(
//        named("readNBytes")
//            .and(takesArguments(0))
//            .and(takesArgument(0, is(byte[].class)))
//            .and(takesArgument(1, is(int.class)))
//            .and(takesArgument(2, is(int.class)))
//            .and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadNBytes");
//    transformers.put(
//        named("available").and(takesArguments(0)).and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_Available");
//
//    // ServletInputStream methods
//    transformers.put(
//        named("readLine")
//            .and(takesArguments(3))
//            .and(takesArgument(0, is(byte[].class)))
//            .and(takesArgument(1, is(int.class)))
//            .and(takesArgument(2, is(int.class)))
//            .and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArray");
//    //     servlet 3.1 API, but since we do not call it directly muzzle
//    transformers.put(
//        named("isFinished").and(takesArguments(0)).and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$ServletInputStream_IsFinished");
    return transformers;
  }

  static class OutputStream_write {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(@Advice.This ServletOutputStream thizz, @Advice.Argument(0) int b) {
      System.out.println("ServletOutput stream write enter");
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(ServletOutputStream.class);
      if (callDepth > 0) {
        return;
      }
      Metadata metadata = InstrumentationContext.get(ServletOutputStream.class, Metadata.class)
          .get(thizz);
      if (metadata != null) {
        metadata.boundedByteArrayOutputStream.write(b);
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      CallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
    }
  }

  static class OutputStream_writeByteArr {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter() {
      System.out.println("ServletOutputStream write array");
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
    }
  }

  static class OutputStream_writeByteArrOffset {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This ServletOutputStream thizz,
        @Advice.Argument(0) byte b[],
        @Advice.Argument(1) int off,
        @Advice.Argument(2) int len) {
      System.out.printf("ServletOutputStream write array offset: %s", new String(b));
      System.out.println(b.length);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
    }
  }

  static class ServletOutputStream_print {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(@Advice.This ServletOutputStream thizz, @Advice.Argument(0) String s)
        throws IOException {
      System.out.println("\n\n\n ---> print enter");
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(ServletOutputStream.class);
      if (callDepth > 0) {
        return ;
      }
      Metadata metadata = InstrumentationContext.get(ServletOutputStream.class, Metadata.class)
          .get(thizz);
      if (metadata != null) {
        String bodyPart = s == null ? "null" : s;
        metadata.boundedByteArrayOutputStream.write(s.getBytes());
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      CallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
    }
  }

  static class OutputStream_close {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter()
        throws UnsupportedEncodingException {
      System.out.println("ServletOutputStream close");
//      Metadata metadata = InstrumentationContext.get(ServletOutputStream.class, Metadata.class)
//          .get(thizz);
//      if (metadata != null) {
//        String responseBody = metadata.boundedByteArrayOutputStream.toStringWithSuppliedCharset();
//        metadata.span.setAttribute(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY.getKey(), responseBody);
//      }
    }
  }

  static class OutputStream_flush {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(@Advice.This ServletOutputStream thizz) {
      System.out.println("ServletOutputStream flush");
      System.out.println(thizz.getClass().getCanonicalName());
    }
  }
}
