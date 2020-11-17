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

package io.opentelemetry.instrumentation.hypertrace.grpc.v1_5.server;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import io.grpc.ServerInterceptor;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class GrpcServerBodyInstrumentation extends Instrumenter.Default {

  public GrpcServerBodyInstrumentation() {
    super("grpc");
  }

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "io.opentelemetry.instrumentation.hypertrace.grpc.v1_5.GrpcSpanDecorator",
      "io.opentelemetry.instrumentation.hypertrace.grpc.v1_5.InstrumentationName",
      packageName + ".GrpcServerInterceptor",
      packageName + ".GrpcServerInterceptor$TracingServerCall",
      packageName + ".GrpcServerInterceptor$TracingServerCallListener",
      packageName + ".NoopServerCallListener"
    };
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("io.grpc.internal.AbstractServerImplBuilder");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isMethod().and(named("build")),
        GrpcServerBodyInstrumentation.class.getName() + "$AddInterceptorAdvice");
  }

  public static class AddInterceptorAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void addInterceptor(
        @Advice.FieldValue("interceptors") List<ServerInterceptor> interceptors) {
      boolean shouldRegister = true;
      for (ServerInterceptor interceptor : interceptors) {
        if (interceptor instanceof GrpcServerInterceptor) {
          shouldRegister = false;
          break;
        }
      }
      if (shouldRegister) {
        // Interceptors run in the reverse order in which they are added
        // OTEL interceptor is last
        interceptors.add(interceptors.size() - 1, new GrpcServerInterceptor());
      }
    }
  }
}
