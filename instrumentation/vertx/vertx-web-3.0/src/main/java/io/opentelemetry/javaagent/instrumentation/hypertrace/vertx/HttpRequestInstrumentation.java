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

package io.opentelemetry.javaagent.instrumentation.hypertrace.vertx;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.field.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.vertx.client.Contexts;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceCallDepthThreadLocalMap;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;

public class HttpRequestInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("io.vertx.core.http.HttpClientRequest");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("io.vertx.core.http.HttpClientRequest"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(named("write").and(takesArgument(0, is(String.class)))),
        HttpRequestInstrumentation.class.getName() + "$WriteRequestAdvice_string");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(
                named("write")
                    .and(takesArguments(1))
                    .and(takesArgument(0, named("io.vertx.core.buffer.Buffer")))),
        HttpRequestInstrumentation.class.getName() + "$WriteRequestAdvice_buffer");

    transformer.applyAdviceToMethod(
        isMethod().and(named("end").and(takesArguments(0))),
        HttpRequestInstrumentation.class.getName() + "$EndRequestAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(named("end").and(takesArgument(0, is(String.class)))),
        HttpRequestInstrumentation.class.getName() + "$EndRequestAdvice_string");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(
                named("end")
                    .and(takesArguments(1))
                    .and(takesArgument(0, named("io.vertx.core.buffer.Buffer")))),
        HttpRequestInstrumentation.class.getName() + "$EndRequestAdvice_buffer");
  }

  public static class EndRequestAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(@Advice.This HttpClientRequest request) {
      int callDepth = HypertraceCallDepthThreadLocalMap.incrementCallDepth(HttpClientRequest.class);
      if (callDepth > 0) {
        return;
      }

      Contexts contexts = VirtualField.find(HttpClientRequest.class, Contexts.class).get(request);
      if (contexts == null) {
        return;
      }
      Span span = Span.fromContext(contexts.context);

      BoundedCharArrayWriter buffer =
          VirtualField.find(MultiMap.class, BoundedCharArrayWriter.class).get(request.headers());
      if (buffer != null) {
        span.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, buffer.toString());
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      HypertraceCallDepthThreadLocalMap.decrementCallDepth(HttpClientRequest.class);
    }
  }

  public static class EndRequestAdvice_string {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This HttpClientRequest request, @Advice.Argument(0) String chunk)
        throws IOException {
      int callDepth = HypertraceCallDepthThreadLocalMap.incrementCallDepth(HttpClientRequest.class);
      if (callDepth > 0) {
        return;
      }

      Contexts contexts = VirtualField.find(HttpClientRequest.class, Contexts.class).get(request);
      if (contexts == null) {
        return;
      }
      Span span = Span.fromContext(contexts.context);

      String contentType = request.headers().get("Content-Type");
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      if (instrumentationConfig.httpBody().request()
          && ContentTypeUtils.shouldCapture(contentType)) {
        BoundedCharArrayWriter buffer =
            VirtualField.find(MultiMap.class, BoundedCharArrayWriter.class).get(request.headers());
        if (buffer == null) {
          span.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, chunk);
        } else {
          buffer.write(chunk);
          span.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, buffer.toString());
        }
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      HypertraceCallDepthThreadLocalMap.decrementCallDepth(HttpClientRequest.class);
    }
  }

  public static class EndRequestAdvice_buffer {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This HttpClientRequest request, @Advice.Argument(0) Buffer chunk)
        throws IOException {

      int callDepth = HypertraceCallDepthThreadLocalMap.incrementCallDepth(HttpClientRequest.class);
      if (callDepth > 0) {
        return;
      }

      Contexts contexts = VirtualField.find(HttpClientRequest.class, Contexts.class).get(request);
      if (contexts == null) {
        return;
      }
      Span span = Span.fromContext(contexts.context);

      String contentType = request.headers().get("Content-Type");
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      if (instrumentationConfig.httpBody().request()
          && ContentTypeUtils.shouldCapture(contentType)) {

        BoundedCharArrayWriter buffer =
            VirtualField.find(MultiMap.class, BoundedCharArrayWriter.class).get(request.headers());
        if (buffer == null) {
          span.setAttribute(
              HypertraceSemanticAttributes.HTTP_REQUEST_BODY,
              chunk.toString(StandardCharsets.UTF_8.name()));
        } else {
          buffer.write(chunk.toString(StandardCharsets.UTF_8.name()));
          span.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, buffer.toString());
        }
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      HypertraceCallDepthThreadLocalMap.decrementCallDepth(HttpClientRequest.class);
    }
  }

  public static class WriteRequestAdvice_string {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This HttpClientRequest request, @Advice.Argument(0) String chunk)
        throws IOException {

      int callDepth = HypertraceCallDepthThreadLocalMap.incrementCallDepth(HttpClientRequest.class);
      if (callDepth > 0) {
        return;
      }

      String contentType = request.headers().get("Content-Type");
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      if (instrumentationConfig.httpBody().request()
          && ContentTypeUtils.shouldCapture(contentType)) {

        VirtualField<MultiMap, BoundedCharArrayWriter> contextStore =
            VirtualField.find(MultiMap.class, BoundedCharArrayWriter.class);
        BoundedCharArrayWriter buffer = contextStore.get(request.headers());
        if (buffer == null) {
          buffer = BoundedBuffersFactory.createWriter();
          contextStore.set(request.headers(), buffer);
        }
        buffer.write(chunk);
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      HypertraceCallDepthThreadLocalMap.decrementCallDepth(HttpClientRequest.class);
    }
  }

  public static class WriteRequestAdvice_buffer {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This HttpClientRequest request, @Advice.Argument(0) Buffer chunk)
        throws IOException {

      int callDepth = HypertraceCallDepthThreadLocalMap.incrementCallDepth(HttpClientRequest.class);
      if (callDepth > 0) {
        return;
      }

      String contentType = request.headers().get("Content-Type");
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      if (instrumentationConfig.httpBody().request()
          && ContentTypeUtils.shouldCapture(contentType)) {

        VirtualField<MultiMap, BoundedCharArrayWriter> contextStore =
            VirtualField.find(MultiMap.class, BoundedCharArrayWriter.class);
        BoundedCharArrayWriter buffer = contextStore.get(request.headers());
        if (buffer == null) {
          buffer = BoundedBuffersFactory.createWriter();
          contextStore.set(request.headers(), buffer);
        }
        buffer.write(chunk.toString(StandardCharsets.UTF_8.name()));
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit() {
      HypertraceCallDepthThreadLocalMap.decrementCallDepth(HttpClientRequest.class);
    }
  }
}
