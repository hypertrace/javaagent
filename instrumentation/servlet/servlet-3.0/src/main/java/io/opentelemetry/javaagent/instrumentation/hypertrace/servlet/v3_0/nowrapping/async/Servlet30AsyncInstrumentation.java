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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.async;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.servlet.ServletHelper;
import io.opentelemetry.javaagent.instrumentation.servlet.v3_0.Servlet3Accessor;
import io.opentelemetry.javaagent.instrumentation.servlet.v3_0.Servlet3Singletons;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.HypertraceCallDepthThreadLocalMap;
import org.hypertrace.agent.core.instrumentation.SpanAndObjectPair;
import org.hypertrace.agent.core.instrumentation.buffer.*;

public final class Servlet30AsyncInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("javax.servlet.Servlet");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("javax.servlet.ServletRequest"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("startAsync").and(returns(named("javax.servlet.AsyncContext"))).and(isPublic()),
        Servlet30AsyncInstrumentation.class.getName() + "$StartAsyncAdvice");
  }

  static final class StartAsyncAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void startAsyncEnter() {
      // This allows to detect the outermost invocation of startAsync in method exit
      HypertraceCallDepthThreadLocalMap.incrementCallDepth(AsyncContext.class);
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void startAsyncExit(@Advice.This ServletRequest servletRequest) {
      int callDepth = HypertraceCallDepthThreadLocalMap.decrementCallDepth(AsyncContext.class);
      if (callDepth != 0) {
        // This is not the outermost invocation, ignore.
        return;
      }

      // response context to capture body and clear the context
      VirtualField<HttpServletResponse, SpanAndObjectPair> responseContextStore =
          VirtualField.find(HttpServletResponse.class, SpanAndObjectPair.class);
      VirtualField<ServletOutputStream, BoundedByteArrayOutputStream> outputStreamContextStore =
          VirtualField.find(ServletOutputStream.class, BoundedByteArrayOutputStream.class);
      VirtualField<PrintWriter, BoundedCharArrayWriter> writerContextStore =
          VirtualField.find(PrintWriter.class, BoundedCharArrayWriter.class);

      // request context to clear body buffer
      VirtualField<HttpServletRequest, SpanAndObjectPair> requestContextStore =
          VirtualField.find(HttpServletRequest.class, SpanAndObjectPair.class);
      VirtualField<ServletInputStream, ByteBufferSpanPair> inputStreamContextStore =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class);
      VirtualField<BufferedReader, CharBufferSpanPair> readerContextStore =
          VirtualField.find(BufferedReader.class, CharBufferSpanPair.class);
      VirtualField<HttpServletRequest, StringMapSpanPair> urlEncodedMapContextStore =
          VirtualField.find(HttpServletRequest.class, StringMapSpanPair.class);

      if (servletRequest instanceof HttpServletRequest) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        Servlet3Accessor accessor = Servlet3Accessor.INSTANCE;
        if (accessor.getRequestAttribute(request, HYPERTRACE_ASYNC_LISTENER_ATTRIBUTE) == null) {
          ServletHelper<HttpServletRequest, HttpServletResponse> helper =
              Servlet3Singletons.helper();
          accessor.addRequestAsyncListener(
              request,
              new BodyCaptureAsyncListener(
                  new AtomicBoolean(),
                  responseContextStore,
                  outputStreamContextStore,
                  writerContextStore,
                  requestContextStore,
                  inputStreamContextStore,
                  readerContextStore,
                  urlEncodedMapContextStore,
                  request),
              helper.getAsyncListenerResponse(request));
          accessor.setRequestAttribute(request, HYPERTRACE_ASYNC_LISTENER_ATTRIBUTE, true);
        }
      }
    }

    public static final String HYPERTRACE_ASYNC_LISTENER_ATTRIBUTE = "org.hypertrace.AsyncListener";
  }
}
