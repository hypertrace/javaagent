package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.response;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.request.HttpRequestInstrumentationUtils;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.Metadata;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.config.HypertraceConfig;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;

public class ServletResponseInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(named("javax.servlet.ServletResponse"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> matchers = new HashMap<>();
    matchers.put(
        named("getOutputStream")
            .and(takesArguments(0))
            .and(returns(named("javax.servlet.ServletOutputStream")))
            .and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$ServletResponse_getOutputStream");
    matchers.put(
        named("getWriter")
            .and(takesArguments(0))
            .and(returns(BufferedReader.class))
            .and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$ServletResponse_getWriter_advice");
    return matchers;
  }

  static class ServletResponse_getOutputStream {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.This ServletResponse servletResponse,
        @Advice.Return ServletOutputStream servletOutputStream) {
      System.out.println("getting response output stream");

      if (!(servletResponse instanceof HttpServletResponse)) {
        return;
      }
      HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

      ContextStore<ServletOutputStream, Metadata> contextStore =
          InstrumentationContext.get(ServletOutputStream.class, Metadata.class);
      if (contextStore.get(servletOutputStream) != null) {
        // getOutputStream() can be called multiple times
        return;
      }

      // span is added in servlet/filter instrumentation if data capture is enabled and it is the
      // right content
      Span requestSpan =
          InstrumentationContext.get(HttpServletResponse.class, Span.class).get(httpServletResponse);
      if (requestSpan == null) {
        return;
      }

      // do not capture if data capture is disabled or not supported content type
      AgentConfig agentConfig = HypertraceConfig.get();
      String contentType = httpServletResponse.getContentType();
      if (agentConfig.getDataCapture().getHttpBody().getResponse().getValue()
          && ContentTypeUtils.shouldCapture(contentType)) {
        System.out.println("Adding metadata for response");
        Metadata metadata =
            HttpRequestInstrumentationUtils.createResponseMetadata(httpServletResponse, requestSpan);
        contextStore.put(servletOutputStream, metadata);
//
//        ContextStore<HttpServletResponse, Metadata> responseContext = InstrumentationContext
//            .get(HttpServletResponse.class, Metadata.class);
//        responseContext.put(httpServletResponse, metadata);
      }
    }
  }

  static class ServletResponse_getWriter_advice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit() {
      System.out.println("Getting printWriter from response");
    }
  }
}
