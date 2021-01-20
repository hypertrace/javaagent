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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.server;

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
import javax.servlet.ServletInputStream;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.core.instrumentation.buffer.ByteBufferSpanPair;

// added explicitly in META-INF/services/...
// @AutoService(InstrumentationModule.class)
public class ServletInputStreamContextAccessInstrumentationModule extends InstrumentationModule {

  public ServletInputStreamContextAccessInstrumentationModule() {
    super("test-servlet-input-stream");
  }

  @Override
  protected boolean defaultEnabled() {
    return true;
  }

  @Override
  protected Map<String, String> contextStore() {
    Map<String, String> context = new HashMap<>();
    context.put("javax.servlet.ServletInputStream", ByteBufferSpanPair.class.getName());
    return context;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new InputStreamTriggerInstrumentation());
  }

  class InputStreamTriggerInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return named("org.ServletInputStreamContextAccess");
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      Map<Junction<MethodDescription>, String> matchers = new HashMap<>();
      matchers.put(
          named("addToContext").and(takesArguments(2)).and(isPublic()),
          ServletInputStreamContextAccessInstrumentationModule.class.getName() + "$TestAdvice");
      return matchers;
    }
  }

  static class TestAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.Argument(0) ServletInputStream servletInputStream,
        @Advice.Argument(1) ByteBufferSpanPair metadata) {
      ContextStore<ServletInputStream, ByteBufferSpanPair> contextStore =
          InstrumentationContext.get(ServletInputStream.class, ByteBufferSpanPair.class);
      contextStore.put(servletInputStream, metadata);
    }
  }
}
