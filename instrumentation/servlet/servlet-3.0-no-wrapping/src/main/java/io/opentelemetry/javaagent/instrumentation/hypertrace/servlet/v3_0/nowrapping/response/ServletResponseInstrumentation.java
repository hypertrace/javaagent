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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.response;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.config.HypertraceConfig;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeCharsetUtils;
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
        named("getWriter").and(takesArguments(0)).and(returns(PrintWriter.class)).and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$ServletResponse_getWriter_advice");
    return matchers;
  }

  static class ServletResponse_getOutputStream {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static HttpServletResponse enter(@Advice.This ServletResponse servletResponse) {
      if (!(servletResponse instanceof HttpServletResponse)) {
        return null;
      }
      HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
      if (httpServletResponse instanceof HttpServletResponseWrapper) {
        System.out.println("response is wrapper");
        return null;
      }
      System.out.println("response is NOT wrapper");

      // the getReader method might call getInputStream
      CallDepthThreadLocalMap.incrementCallDepth(ServletResponse.class);
      return httpServletResponse;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Enter HttpServletResponse httpServletResponse,
        @Advice.Return ServletOutputStream servletOutputStream) {

      if (httpServletResponse == null) {
        return;
      }

      int callDepth = CallDepthThreadLocalMap.decrementCallDepth(ServletResponse.class);
      if (callDepth > 0) {
        return;
      }

      ContextStore<ServletOutputStream, BoundedByteArrayOutputStream> contextStore =
          InstrumentationContext.get(ServletOutputStream.class, BoundedByteArrayOutputStream.class);
      if (contextStore.get(servletOutputStream) != null) {
        // getOutputStream() can be called multiple times
        return;
      }

      // do not capture if data capture is disabled or not supported content type
      AgentConfig agentConfig = HypertraceConfig.get();
      String contentType = httpServletResponse.getContentType();
      if (agentConfig.getDataCapture().getHttpBody().getResponse().getValue()
          && ContentTypeUtils.shouldCapture(contentType)) {

        System.out.printf(
            "created response byte buffer: %s", httpServletResponse.getClass().getName());

        String charsetStr = httpServletResponse.getCharacterEncoding();
        Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);
        BoundedByteArrayOutputStream buffer = BoundedBuffersFactory.createStream(charset);
        contextStore.put(servletOutputStream, buffer);
        // override the metadata that is used by the OutputStream instrumentation
      }
    }
  }

  static class ServletResponse_getWriter_advice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static HttpServletResponse enter(@Advice.This ServletResponse servletResponse) {
      if (!(servletResponse instanceof HttpServletResponse)) {
        return null;
      }
      HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
      if (httpServletResponse instanceof HttpServletResponseWrapper) {
        System.out.println("response is wrapper");
        return null;
      }
      System.out.println("response is NOT wrapper");

      // the getWriter method might call getInputStream
      CallDepthThreadLocalMap.incrementCallDepth(ServletResponse.class);
      return httpServletResponse;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Enter HttpServletResponse httpServletResponse,
        @Advice.Return PrintWriter printWriter) {

      if (httpServletResponse == null) {
        return;
      }

      int callDepth = CallDepthThreadLocalMap.decrementCallDepth(ServletResponse.class);
      if (callDepth > 0) {
        return;
      }

      ContextStore<PrintWriter, BoundedCharArrayWriter> contextStore =
          InstrumentationContext.get(PrintWriter.class, BoundedCharArrayWriter.class);
      if (contextStore.get(printWriter) != null) {
        // getWriter() can be called multiple times
        return;
      }

      // do not capture if data capture is disabled or not supported content type
      AgentConfig agentConfig = HypertraceConfig.get();
      String contentType = httpServletResponse.getContentType();
      if (agentConfig.getDataCapture().getHttpBody().getResponse().getValue()
          && ContentTypeUtils.shouldCapture(contentType)) {

        System.out.printf(
            "created response char buffer: %s", httpServletResponse.getClass().getName());

        BoundedCharArrayWriter writer = BoundedBuffersFactory.createWriter();
        contextStore.put(printWriter, writer);
      }
    }
  }
}
