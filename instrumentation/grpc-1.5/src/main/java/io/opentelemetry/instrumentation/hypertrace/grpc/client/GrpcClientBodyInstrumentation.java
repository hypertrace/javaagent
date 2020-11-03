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

package io.opentelemetry.instrumentation.hypertrace.grpc.client;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.extendsClass;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.declaresField;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import io.grpc.ClientInterceptor;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class GrpcClientBodyInstrumentation extends Instrumenter.Default {

  public GrpcClientBodyInstrumentation() {
    super("grpc");
  }

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "io.opentelemetry.instrumentation.grpc.v1_5.common.GrpcHelper",
      "io.opentelemetry.instrumentation.grpc.v1_5.client.GrpcClientTracer",
      "io.opentelemetry.instrumentation.grpc.v1_5.client.GrpcInjectAdapter",
      "io.opentelemetry.instrumentation.grpc.v1_5.client.TracingClientInterceptor",
      "io.opentelemetry.instrumentation.grpc.v1_5.client.TracingClientInterceptor$TracingClientCall",
      "io.opentelemetry.instrumentation.grpc.v1_5.client.TracingClientInterceptor$TracingClientCallListener",
      "io.opentelemetry.instrumentation.grpc.v1_5.server.GrpcExtractAdapter",
      "io.opentelemetry.instrumentation.grpc.v1_5.server.GrpcServerTracer",
      "io.opentelemetry.instrumentation.grpc.v1_5.server.TracingServerInterceptor",
      "io.opentelemetry.instrumentation.grpc.v1_5.server.TracingServerInterceptor$TracingServerCall",
      "io.opentelemetry.instrumentation.grpc.v1_5.server.TracingServerInterceptor$TracingServerCallListener",
      "org.hypertrace.agent.core.HypertraceSemanticAttributes",
      "io.opentelemetry.instrumentation.hypertrace.grpc.GrpcTracer",
      "io.opentelemetry.instrumentation.hypertrace.grpc.GrpcSpanDecorator",
      "io.opentelemetry.instrumentation.hypertrace.grpc.InstrumentationName",
      packageName + ".GrpcClientInterceptor"
    };
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return extendsClass(named("io.grpc.ManagedChannelBuilder"))
        .and(declaresField(named("interceptors")));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isMethod().and(named("build")),
        GrpcClientBodyInstrumentation.class.getName() + "$AddInterceptorAdvice");
  }

  public static class AddInterceptorAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void addInterceptor(
        @Advice.FieldValue("interceptors") List<ClientInterceptor> interceptors) {
      boolean shouldRegister = true;
      for (ClientInterceptor interceptor : interceptors) {
        if (interceptor instanceof GrpcClientInterceptor) {
          shouldRegister = false;
          break;
        }
      }
      if (shouldRegister) {
        interceptors.add(0, new GrpcClientInterceptor());
      }
    }
  }
}
