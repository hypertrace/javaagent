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

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.extension.matcher.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge.currentContext;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient.ApacheHttpAsyncClientInstrumentation.DelegatingRequestProducer;
import io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient.ApacheHttpAsyncClientInstrumentation.WrappedFutureCallback;
import io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpclient.v4_0.ApacheHttpClientUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

@AutoService(InstrumentationModule.class)
public class ApacheAsyncClientInstrumentationModule extends InstrumentationModule {

  public ApacheAsyncClientInstrumentationModule() {
    super(
        ApacheAsyncHttpClientInstrumentationName.PRIMARY,
        ApacheAsyncHttpClientInstrumentationName.OTHER);
  }

  @Override
  public int order() {
    return 1;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new HttpAsyncClientInstrumentation());
  }

  static class HttpAsyncClientInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
      return hasClassesNamed("org.apache.http.nio.client.HttpAsyncClient");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return implementsInterface(named("org.apache.http.nio.client.HttpAsyncClient"));
    }

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
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
        @Advice.Argument(value = 0, readOnly = false) HttpAsyncRequestProducer requestProducer,
        @Advice.Argument(value = 2) HttpContext httpContext,
        @Advice.Argument(value = 3, readOnly = false) FutureCallback<?> futureCallback) {
      if (requestProducer instanceof DelegatingRequestProducer) {
        Context context = currentContext();
        BodyCaptureDelegatingCallback<?> bodyCaptureDelegatingCallback =
            new BodyCaptureDelegatingCallback<>(context, httpContext, futureCallback);
        futureCallback = bodyCaptureDelegatingCallback;
        requestProducer =
            new DelegatingCaptureBodyRequestProducer(
                context, requestProducer, bodyCaptureDelegatingCallback);
      }
    }
  }

  public static class DelegatingCaptureBodyRequestProducer extends DelegatingRequestProducer {

    final Context context;

    public DelegatingCaptureBodyRequestProducer(
        Context context,
        HttpAsyncRequestProducer delegate,
        WrappedFutureCallback<?> wrappedFutureCallback) {
      super(context, delegate, wrappedFutureCallback);
      this.context = context;
    }

    @Override
    public HttpRequest generateRequest() throws IOException, HttpException {
      HttpRequest request = super.generateRequest();
      ApacheHttpClientUtils.traceRequest(Span.fromContext(context), request);
      return request;
    }
  }

  public static class BodyCaptureDelegatingCallback<T> extends WrappedFutureCallback<T> {

    final Context context;
    final HttpContext httpContext;

    public BodyCaptureDelegatingCallback(
        Context context, HttpContext httpContext, FutureCallback<T> delegate) {
      super(context, httpContext, delegate);
      this.context = context;
      this.httpContext = httpContext;
    }

    @Override
    public void completed(T result) {
      HttpResponse httpResponse = getResponse(httpContext);
      ApacheHttpClientUtils.traceResponse(Span.fromContext(context), httpResponse);
      super.completed(result);
    }

    @Override
    public void failed(Exception ex) {
      HttpResponse httpResponse = getResponse(httpContext);
      ApacheHttpClientUtils.traceResponse(Span.fromContext(context), httpResponse);
      super.failed(ex);
    }

    @Override
    public void cancelled() {
      HttpResponse httpResponse = getResponse(httpContext);
      ApacheHttpClientUtils.traceResponse(Span.fromContext(context), httpResponse);
      super.cancelled();
    }

    private static HttpResponse getResponse(HttpContext context) {
      return (HttpResponse) context.getAttribute(HttpCoreContext.HTTP_RESPONSE);
    }
  }
}
