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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletInputStream;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;

public class PrintWriterInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(named("java.io.PrintWriter"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        named("write").and(takesArguments(3))
            .and(takesArgument(0, is(char[].class)))
            .and(takesArgument(1, is(int.class)))
            .and(takesArgument(2, is(int.class)))
            .and(isPublic()),
        PrintWriterInstrumentation.class.getName() + "$Writer_write");
    return transformers;
  }

  static class Writer_write {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Metadata enter(@Advice.This PrintWriter thizz, @Advice.Argument(0) char[] buf, @Advice.Argument(1) int off, @Advice.Argument(2) int len) {
      System.out.println("\n\n\n ---> print enter");
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(PrintWriter.class);
      if (callDepth > 0) {
        return null;
      }
      Metadata metadata = InstrumentationContext.get(PrintWriter.class, Metadata.class)
          .get(thizz);
      if (metadata != null) {
//        metadata.boundedByteArrayOutputStream.
      }

      return metadata;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Argument(0) String str,
        @Advice.Enter Metadata metadata) throws IOException {
      System.out.println("print exit");
      CallDepthThreadLocalMap.decrementCallDepth(ServletInputStream.class);
      if (metadata == null) {
        return;
      }
      String bodyPart = str == null ? "null" : str;
      metadata.boundedByteArrayOutputStream.write(bodyPart.getBytes());
    }
  }
}
