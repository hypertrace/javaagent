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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.config.HypertraceConfig;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.utils.ContentLengthUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeCharsetUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;

public class ServletRequestInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(named("javax.servlet.ServletRequest"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> matchers = new HashMap<>();
    matchers.put(
        named("getInputStream")
            .and(takesArguments(0))
            .and(returns(named("javax.servlet.ServletInputStream")))
            .and(isPublic()),
        ServletRequestInstrumentation.class.getName() + "$ServletRequest_getInputStream_advice");
    matchers.put(
        named("getReader")
            .and(takesArguments(0))
            .and(returns(BufferedReader.class))
            .and(isPublic()),
        ServletRequestInstrumentation.class.getName() + "$ServletRequest_getReader_advice");
    return matchers;
  }

  static class ServletRequest_getInputStream_advice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.This ServletRequest servletRequest,
        @Advice.Return ServletInputStream servletInputStream) {
      System.out.println("\n\n\n\n ---> getInputStream");

      if (!(servletRequest instanceof HttpServletRequest)) {
        return;
      }
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

      // TODO consider moving this to servlet advice to avoid adding data to the
      // InstrumentationContext
      AgentConfig agentConfig = HypertraceConfig.get();
      String contentType = httpServletRequest.getContentType();
      if (agentConfig.getDataCapture().getHttpBody().getRequest().getValue()
          && !ContentTypeUtils.shouldCapture(contentType)) {
        return;
      }

      // add input stream to a map to indicate it should be instrumented
      ContextStore<HttpServletRequest, Span> requestSpanContextStore =
          InstrumentationContext.get(HttpServletRequest.class, Span.class);
      System.out.printf(
          "requestSpanContextStore in ServletRequest_getInputStream_advice: %s\n",
          requestSpanContextStore);
      Span requestSpan = requestSpanContextStore.get(httpServletRequest);
      if (requestSpan == null) {
        return;
      }
      System.out.println("AAAAA");

      String charsetStr = ContentTypeUtils.parseCharset(contentType);
      Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);
      int contentLength = httpServletRequest.getContentLength();
      if (contentLength < 0) {
        contentLength = ContentLengthUtils.DEFAULT;
      }

      Metadata metadata = new Metadata(requestSpan, httpServletRequest, BoundedBuffersFactory.createStream(contentLength, charset));
      InstrumentationContext.get(ServletInputStream.class, Metadata.class)
          .put(servletInputStream, metadata);
      System.out.println("BBBB");
    }
  }

  static class ServletRequest_getReader_advice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.This ServletRequest servletRequest, @Advice.Return BufferedReader reader) {
      // TODO
    }
  }
}
