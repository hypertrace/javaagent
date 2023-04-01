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

import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.tooling.muzzle.InstrumentationModuleMuzzle;
import io.opentelemetry.javaagent.tooling.muzzle.VirtualFieldMappingsBuilder;
import io.opentelemetry.javaagent.tooling.muzzle.references.ClassRef;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.SpanAndBuffer;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;

// SPI explicitly added in META-INF/services/...
public class ContextAccessorInstrumentationModule extends InstrumentationModule
    implements InstrumentationModuleMuzzle {

  @Override
  public Map<String, ClassRef> getMuzzleReferences() {
    return Collections.emptyMap();
  }

  @Override
  public void registerMuzzleVirtualFields(VirtualFieldMappingsBuilder builder) {
    builder
        .register("java.io.InputStream", SpanAndBuffer.class.getName())
        .register("java.io.OutputStream", BoundedByteArrayOutputStream.class.getName());
  }

  @Override
  public List<String> getMuzzleHelperClassNames() {
    return Collections.emptyList();
  }

  public ContextAccessorInstrumentationModule() {
    super("test-context-accessor");
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
      VirtualField.find(InputStream.class, SpanAndBuffer.class).set(inputStream, spanAndBuffer);
    }
  }

  static class AddToOutputStreamContextAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.Argument(0) OutputStream outputStream,
        @Advice.Argument(1) BoundedByteArrayOutputStream buffer) {
      VirtualField.find(OutputStream.class, BoundedByteArrayOutputStream.class)
          .set(outputStream, buffer);
    }
  }
}
