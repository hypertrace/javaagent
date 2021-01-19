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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.response;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.ByteBufferMetadata;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.request.HttpRequestInstrumentationUtils;
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

      ContextStore<ServletOutputStream, ByteBufferMetadata> contextStore =
          InstrumentationContext.get(ServletOutputStream.class, ByteBufferMetadata.class);
      if (contextStore.get(servletOutputStream) != null) {
        // getOutputStream() can be called multiple times
        return;
      }

      /** TODO what if the response is a wrapper? - it needs to be unwrapped */
      ContextStore<HttpServletResponse, ByteBufferMetadata> responseContext =
          InstrumentationContext.get(HttpServletResponse.class, ByteBufferMetadata.class);
      ByteBufferMetadata responseMetadata = responseContext.get(httpServletResponse);
      if (responseMetadata == null) {
        return;
      }

      // do not capture if data capture is disabled or not supported content type
      AgentConfig agentConfig = HypertraceConfig.get();
      String contentType = httpServletResponse.getContentType();
      if (agentConfig.getDataCapture().getHttpBody().getResponse().getValue()
          && ContentTypeUtils.shouldCapture(contentType)) {
        System.out.println("Adding metadata for response");
        ByteBufferMetadata metadata =
            HttpRequestInstrumentationUtils.createResponseMetadata(
                httpServletResponse, responseMetadata.span);
        contextStore.put(servletOutputStream, metadata);
        // override the metadata that is used by the OutputStream instrumentation
        responseContext.put(httpServletResponse, metadata);
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
