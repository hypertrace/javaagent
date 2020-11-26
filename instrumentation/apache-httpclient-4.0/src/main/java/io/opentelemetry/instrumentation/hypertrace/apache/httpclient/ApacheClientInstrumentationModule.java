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

package io.opentelemetry.instrumentation.hypertrace.apache.httpclient;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.hypertrace.agent.core.ContentTypeUtils;
import org.hypertrace.agent.core.GlobalContextHolder;
import org.hypertrace.agent.core.GlobalContextHolder.SpanAndBuffer;

// @AutoService(InstrumentationModule.class)
public class ApacheClientInstrumentationModule extends InstrumentationModule {

  public ApacheClientInstrumentationModule() {
    super(ApacheHttpClientInstrumentationName.PRIMARY, ApacheHttpClientInstrumentationName.OTHER);
  }

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(new HttpEntityInstrumentation(), new ApacheClientInstrumentation());
  }

  static class ApacheClientInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return implementsInterface(named("org.apache.http.client.HttpClient"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();

      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArguments(1))
              .and(takesArgument(0, named("org.apache.http.client.methods.HttpUriRequest"))),
          HttpClient_ExecuteAdvice.class.getName());

      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArguments(2))
              .and(takesArgument(0, named("org.apache.http.client.methods.HttpUriRequest")))
              .and(takesArgument(1, named("org.apache.http.client.ResponseHandler"))),
          HttpClient_ExecuteAdvice.class.getName());

      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArguments(3))
              .and(takesArgument(0, named("org.apache.http.client.methods.HttpUriRequest")))
              .and(takesArgument(1, named("org.apache.http.client.ResponseHandler")))
              .and(takesArgument(2, named("org.apache.http.protocol.HttpContext"))),
          HttpClient_ExecuteAdvice.class.getName());
      return transformers;
    }
  }

  static class HttpClient_ExecuteAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void readEnd(@Advice.Return Object response) {
      if (response instanceof HttpResponse) {
        Span currentSpan = Java8BytecodeBridge.currentSpan();
        HttpResponse httpResponse = (HttpResponse) response;
        HttpEntity entity = httpResponse.getEntity();
        System.out.println("\n\nIt is entity");
        GlobalContextHolder.objectToSpanMap.put(entity, currentSpan);
      } else {
        System.out.println("\n\nIt is not HttpResponse #execute");
      }
    }
  }

  static class HttpEntityInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return implementsInterface(named("org.apache.http.HttpEntity"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
      transformers.put(
          named("getContent").and(takesArguments(0)).and(returns(java.io.InputStream.class)),
          HttpEntity_GetContentAdvice.class.getName());
      return transformers;
    }
  }

  static class HttpEntity_GetContentAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void readEnd(
        @Advice.Return java.io.InputStream inputStream, @Advice.This HttpEntity thizz) {
      // here the Span.current() has already been finished
      Span clientSpan = GlobalContextHolder.objectToSpanMap.get(thizz);
      // HttpEntity might be wrapped multiple times
      // this ensures that the advice runs only for the most outer one
      // the returned inputStream is put into globally accessible map
      // The InputStream instrumentation then checks if the input stream is in the map and only
      // then intercepts the reads.
      if (clientSpan == null) {
        return;
      }

      if (!ContentTypeUtils.shouldCapture(thizz.getContentType().getValue())) {
        return;
      }

      System.out.printf("InputStream %s:, will be buffered\n", thizz);
      SpanAndBuffer spanAndBuffer = new SpanAndBuffer(clientSpan, ByteBuffer.allocate(100));
      GlobalContextHolder.objectToSpanAndBufferMap.put(inputStream, spanAndBuffer);
    }
  }
}
