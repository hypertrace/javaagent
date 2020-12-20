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

package io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpasyncclient;

import static io.opentelemetry.javaagent.tooling.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient.ApacheHttpAsyncClientInstrumentation.DelegatingRequestProducer;
import io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient.DelegatingRequestAccessor;
import io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpclient.v4_0.ApacheHttpClientUtils;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;

@AutoService(InstrumentationModule.class)
public class ApacheAsyncClientInstrumentationModule extends InstrumentationModule {

  public ApacheAsyncClientInstrumentationModule() {
    super(
        ApacheAsyncHttpClientInstrumentationName.PRIMARY,
        ApacheAsyncHttpClientInstrumentationName.OTHER);
  }

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new HttpAsyncClientInstrumentation());
  }

  class HttpAsyncClientInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
      return hasClassesNamed("org.apache.http.nio.client.HttpAsyncClient");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return implementsInterface(named("org.apache.http.nio.client.HttpAsyncClient"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      return singletonMap(
          isMethod()
              .and(named("execute"))
              .and(takesArguments(4))
              .and(takesArgument(0, named("org.apache.http.nio.protocol.HttpAsyncRequestProducer")))
              .and(
                  takesArgument(1, named("org.apache.http.nio.protocol.HttpAsyncResponseConsumer")))
              .and(takesArgument(2, named("org.apache.http.protocol.HttpContext")))
              .and(takesArgument(3, named("org.apache.http.concurrent.FutureCallback"))),
          ApacheAsyncClientInstrumentationModule.class.getName()
              + "$HttpAsyncClient_execute_Advice");
    }
  }

  public static class HttpAsyncClient_execute_Advice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.Argument(value = 0, readOnly = false) HttpAsyncRequestProducer requestProducer) {
      System.out.println("on enter");
      if (requestProducer instanceof DelegatingRequestProducer) {
        DelegatingRequestProducer delegatingRequestProducer =
            (DelegatingRequestProducer) requestProducer;
        Context context = DelegatingRequestAccessor.get(delegatingRequestProducer);
        requestProducer = new DelegatingCaptureBodyRequestProducer(context, requestProducer);
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.Return Object response) {}
  }

  public static class DelegatingCaptureBodyRequestProducer extends DelegatingRequestProducer {

    final Context context;

    public DelegatingCaptureBodyRequestProducer(
        Context context, HttpAsyncRequestProducer delegate) {
      super(context, delegate);
      this.context = context;
    }

    @Override
    public HttpRequest generateRequest() throws IOException, HttpException {
      System.out.println("generate request");
      HttpRequest request = super.generateRequest();
      ApacheHttpClientUtils.traceRequest(Span.fromContext(context), request);
      return request;
    }
  }
}
