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

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.extension.matcher.ClassLoaderMatcher.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.vertx.client.Contexts;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;

public class HttpRequestHandleInstrumentation implements TypeInstrumentation {

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
    // capture response data
    transformer.applyAdviceToMethod(
        isMethod().and(named("handleResponse")),
        HttpRequestHandleInstrumentation.class.getName() + "$HandleResponseAdvice");
  }

  public static class HandleResponseAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void handleResponseEnter(
        @Advice.This HttpClientRequest request, @Advice.Argument(0) HttpClientResponse response) {

      Contexts contexts =
          InstrumentationContext.get(HttpClientRequest.class, Contexts.class).get(request);
      if (contexts == null) {
        return;
      }
      Span span = Span.fromContext(contexts.context);

      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      if (instrumentationConfig.httpHeaders().request()) {
        for (Map.Entry<String, String> entry : request.headers()) {
          span.setAttribute(
              HypertraceSemanticAttributes.httpRequestHeader(entry.getKey()), entry.getValue());
        }
      }

      if (instrumentationConfig.httpHeaders().response()) {
        for (Map.Entry<String, String> entry : response.headers()) {
          span.setAttribute(
              HypertraceSemanticAttributes.httpResponseHeader(entry.getKey()), entry.getValue());
        }
      }

      String contentType = response.getHeader("Content-Type");
      if (instrumentationConfig.httpBody().response()
          && ContentTypeUtils.shouldCapture(contentType)) {
        InstrumentationContext.get(HttpClientResponse.class, Span.class).put(response, span);
      }
    }
  }
}
