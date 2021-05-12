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

package io.opentelemetry.javaagent.instrumentation.hypertrace.jaxrs.v2_0;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.extendsClass;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.hasInterface;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.ClassLoaderMatcher.hasClassesNamed;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.bootstrap.instrumentation.SpanAndBuffer;
import org.hypertrace.agent.core.bootstrap.instrumentation.buffer.BoundedByteArrayOutputStream;

@AutoService(InstrumentationModule.class)
public class JaxrsClientBodyInstrumentationModule extends InstrumentationModule {

  public JaxrsClientBodyInstrumentationModule() {
    super(JaxrsClientBodyInstrumentationName.PRIMARY, JaxrsClientBodyInstrumentationName.OTHER);
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new JaxrsClientBuilderInstrumentation());
  }

  class JaxrsClientBuilderInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
      return hasClassesNamed("javax.ws.rs.client.ClientBuilder");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return extendsClass(named("javax.ws.rs.client.ClientBuilder"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      return singletonMap(
          named("build").and(returns(hasInterface(named("javax.ws.rs.client.Client")))),
          JaxrsClientBodyInstrumentationModule.class.getName() + "$ClientBuilder_build_Advice");
    }
  }

  static class ClientBuilder_build_Advice {
    @Advice.OnMethodExit
    public static void registerFeature(
        @Advice.Return(typing = Assigner.Typing.DYNAMIC) Client client) {
      // Register on the generated client instead of the builder
      // The build() can be called multiple times and is not thread safe
      // A client is only created once
      // Use lowest priority to run after OTEL filter that controls lifecycle of span
      client.register(JaxrsClientBodyCaptureFilter.class, Integer.MIN_VALUE);
      client.register(
          new JaxrsClientEntityInterceptor(
              InstrumentationContext.get(InputStream.class, SpanAndBuffer.class),
              InstrumentationContext.get(OutputStream.class, BoundedByteArrayOutputStream.class)));
    }
  }
}
