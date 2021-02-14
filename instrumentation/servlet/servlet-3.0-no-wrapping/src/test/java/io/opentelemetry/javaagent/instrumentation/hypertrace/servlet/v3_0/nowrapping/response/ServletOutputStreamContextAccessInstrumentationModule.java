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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.response;

import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;

public class ServletOutputStreamContextAccessInstrumentationModule extends InstrumentationModule {

  public ServletOutputStreamContextAccessInstrumentationModule() {
    super("test-servlet-output-stream");
  }

  @Override
  protected Map<String, String> contextStore() {
    Map<String, String> context = new HashMap<>();
    context.put("javax.servlet.ServletOutputStream", BoundedByteArrayOutputStream.class.getName());
    return context;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new OutputStreamTriggerInstrumentation());
  }

  class OutputStreamTriggerInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return named("org.ServletStreamContextAccess");
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      Map<Junction<MethodDescription>, String> matchers = new HashMap<>();
      matchers.put(
          named("addToOutputStreamContext").and(takesArguments(2)).and(isPublic()),
          ServletOutputStreamContextAccessInstrumentationModule.class.getName() + "$TestAdvice");
      return matchers;
    }
  }

  static class TestAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.Argument(0) ServletOutputStream servletOutputStream,
        @Advice.Argument(1) BoundedByteArrayOutputStream buffer) {
      ContextStore<ServletOutputStream, BoundedByteArrayOutputStream> contextStore =
          InstrumentationContext.get(ServletOutputStream.class, BoundedByteArrayOutputStream.class);
      contextStore.put(servletOutputStream, buffer);
    }
  }
}
