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

package io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpclient.v4_0;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Arrays;
import java.util.List;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.hypertrace.agent.core.instrumentation.HypertraceCallDepthThreadLocalMap;

@AutoService(InstrumentationModule.class)
public class ApacheClientInstrumentationModule extends InstrumentationModule {

  public ApacheClientInstrumentationModule() {
    super(ApacheHttpClientInstrumentationName.PRIMARY, ApacheHttpClientInstrumentationName.OTHER);
  }

  @Override
  public int order() {
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
    public void transform(TypeTransformer transformer) {
      // instrument response
      transformer.applyAdviceToMethod(
          isMethod().and(named("execute")).and(not(isAbstract())),
          ApacheClientInstrumentationModule.class.getName() + "$HttpClient_ExecuteAdvice_response");

      // instrument request
      transformer.applyAdviceToMethod(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArgument(0, hasSuperType(named("org.apache.http.HttpMessage")))),
          ApacheClientInstrumentationModule.class.getName() + "$HttpClient_ExecuteAdvice_request0");
      transformer.applyAdviceToMethod(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArgument(1, hasSuperType(named("org.apache.http.HttpMessage")))),
          ApacheClientInstrumentationModule.class.getName() + "$HttpClient_ExecuteAdvice_request1");
    }
  }

  static class HttpClient_ExecuteAdvice_request0 {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean enter(@Advice.Argument(0) HttpMessage request) {
      int callDepth = HypertraceCallDepthThreadLocalMap.incrementCallDepth(HttpMessage.class);
      if (callDepth > 0) {
        return false;
      }
      ApacheHttpClientUtils.traceRequest(Java8BytecodeBridge.currentSpan(), request);
      return true;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exit(
        @Advice.Enter boolean returnFromEnter, @Advice.Thrown Throwable throwable) {
      if (returnFromEnter) {
        HypertraceCallDepthThreadLocalMap.reset(HttpMessage.class);
      }
    }
  }

  static class HttpClient_ExecuteAdvice_request1 {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean enter(@Advice.Argument(1) HttpMessage request) {
      int callDepth = HypertraceCallDepthThreadLocalMap.incrementCallDepth(HttpMessage.class);
      if (callDepth > 0) {
        return false;
      }
      ApacheHttpClientUtils.traceRequest(Java8BytecodeBridge.currentSpan(), request);
      return true;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exit(
        @Advice.Enter boolean returnFromEnter, @Advice.Thrown Throwable throwable) {
      if (returnFromEnter) {
        HypertraceCallDepthThreadLocalMap.reset(HttpMessage.class);
      }
    }
  }

  static class HttpClient_ExecuteAdvice_response {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean enter() {
      int callDepth = HypertraceCallDepthThreadLocalMap.incrementCallDepth(HttpResponse.class);
      if (callDepth > 0) {
        return false;
      }
      return true;
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.Return Object response, @Advice.Enter boolean returnFromEnter) {
      if (!returnFromEnter) {
        return;
      }

      HypertraceCallDepthThreadLocalMap.reset(HttpResponse.class);
      if (response instanceof HttpResponse) {
        HttpResponse httpResponse = (HttpResponse) response;
        ApacheHttpClientUtils.traceResponse(Java8BytecodeBridge.currentSpan(), httpResponse);
      }
    }
  }
}
