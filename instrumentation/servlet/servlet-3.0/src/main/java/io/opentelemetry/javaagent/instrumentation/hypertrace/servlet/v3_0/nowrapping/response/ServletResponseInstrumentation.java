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

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceCallDepthThreadLocalMap;
import org.hypertrace.agent.core.instrumentation.SpanAndObjectPair;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeCharsetUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;

public class ServletResponseInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("javax.servlet.ServletResponse"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("getOutputStream")
            .and(takesArguments(0))
            .and(returns(named("javax.servlet.ServletOutputStream")))
            .and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$ServletResponse_getOutputStream");
    transformer.applyAdviceToMethod(
        named("getWriter").and(takesArguments(0)).and(returns(PrintWriter.class)).and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$ServletResponse_getWriter_advice");
  }

  static class ServletResponse_getOutputStream {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static HttpServletResponse enter(@Advice.This ServletResponse servletResponse) {
      if (!(servletResponse instanceof HttpServletResponse)) {
        return null;
      }
      // ignore wrappers, the filter/servlet instrumentation gets the captured body from context
      // store
      // by using response as a key and the filter/servlet instrumentation runs early when wrappers
      // are not used.
      HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
      if (httpServletResponse instanceof HttpServletResponseWrapper) {
        return null;
      }

      // the getReader method might call getInputStream
      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletResponse.class);
      return httpServletResponse;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Enter HttpServletResponse httpServletResponse,
        @Advice.Thrown Throwable throwable,
        @Advice.Return ServletOutputStream servletOutputStream) {

      if (httpServletResponse == null) {
        return;
      }

      int callDepth = HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletResponse.class);
      if (callDepth > 0) {
        return;
      }
      if (throwable != null) {
        return;
      }

      VirtualField<ServletOutputStream, BoundedByteArrayOutputStream> contextStore =
          VirtualField.find(ServletOutputStream.class, BoundedByteArrayOutputStream.class);
      if (contextStore.get(servletOutputStream) != null) {
        // getOutputStream() can be called multiple times
        return;
      }

      // do not capture if data capture is disabled or not supported content type
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      String contentType = httpServletResponse.getContentType();
      if (instrumentationConfig.httpBody().response()
          && ContentTypeUtils.shouldCapture(contentType)) {

        String charsetStr = httpServletResponse.getCharacterEncoding();
        Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);
        BoundedByteArrayOutputStream buffer = BoundedBuffersFactory.createStream(charset);
        contextStore.set(servletOutputStream, buffer);
        SpanAndObjectPair spanAndObjectPair = new SpanAndObjectPair(null, null);
        spanAndObjectPair.setAssociatedObject(servletOutputStream);
        VirtualField.find(HttpServletResponse.class, SpanAndObjectPair.class)
            .set(httpServletResponse, spanAndObjectPair);
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
        return null;
      }

      // the getWriter method might call getInputStream
      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletResponse.class);
      return httpServletResponse;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Enter HttpServletResponse httpServletResponse,
        @Advice.Thrown Throwable throwable,
        @Advice.Return PrintWriter printWriter) {

      if (httpServletResponse == null) {
        return;
      }

      int callDepth = HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletResponse.class);
      if (callDepth > 0) {
        return;
      }
      if (throwable != null) {
        return;
      }

      VirtualField<PrintWriter, BoundedCharArrayWriter> contextStore =
          VirtualField.find(PrintWriter.class, BoundedCharArrayWriter.class);
      if (contextStore.get(printWriter) != null) {
        // getWriter() can be called multiple times
        return;
      }

      // do not capture if data capture is disabled or not supported content type
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      String contentType = httpServletResponse.getContentType();
      if (instrumentationConfig.httpBody().response()
          && ContentTypeUtils.shouldCapture(contentType)) {

        BoundedCharArrayWriter writer = BoundedBuffersFactory.createWriter();
        contextStore.set(printWriter, writer);
        SpanAndObjectPair spanAndObjectPair = new SpanAndObjectPair(null, null);
        spanAndObjectPair.setAssociatedObject(printWriter);
        VirtualField.find(HttpServletResponse.class, SpanAndObjectPair.class)
            .set(httpServletResponse, spanAndObjectPair);
      }
    }
  }
}
