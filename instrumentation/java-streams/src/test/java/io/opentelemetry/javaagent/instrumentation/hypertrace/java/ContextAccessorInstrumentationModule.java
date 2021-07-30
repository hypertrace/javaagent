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

package io.opentelemetry.javaagent.instrumentation.hypertrace.java;

import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.SpanAndBuffer;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;

// SPI explicitly added in META-INF/services/...
public class ContextAccessorInstrumentationModule extends InstrumentationModule {

  public ContextAccessorInstrumentationModule() {
    super("test-context-accessor");
  }

  @Override
  public Map<String, String> getMuzzleContextStoreClasses() {
    Map<String, String> contextStore = new HashMap<>();
    contextStore.put("java.io.InputStream", SpanAndBuffer.class.getName());
    contextStore.put("java.io.OutputStream", BoundedByteArrayOutputStream.class.getName());
    return contextStore;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new ContextAccessorInstrumentation());
  }

  class ContextAccessorInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return named("org.ContextAccessor");
    }

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
          named("addToInputStreamContext").and(takesArguments(2)).and(isPublic()),
          ContextAccessorInstrumentationModule.class.getName() + "$AddToInputStreamContextAdvice");
      transformer.applyAdviceToMethod(
          named("addToOutputStreamContext").and(takesArguments(2)).and(isPublic()),
          ContextAccessorInstrumentationModule.class.getName() + "$AddToOutputStreamContextAdvice");
    }
  }

  static class AddToInputStreamContextAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.Argument(0) InputStream inputStream,
        @Advice.Argument(1) SpanAndBuffer spanAndBuffer) {
      InstrumentationContext.get(InputStream.class, SpanAndBuffer.class)
          .put(inputStream, spanAndBuffer);
    }
  }

  static class AddToOutputStreamContextAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.Argument(0) OutputStream outputStream,
        @Advice.Argument(1) BoundedByteArrayOutputStream buffer) {
      InstrumentationContext.get(OutputStream.class, BoundedByteArrayOutputStream.class)
          .put(outputStream, buffer);
    }
  }
}
