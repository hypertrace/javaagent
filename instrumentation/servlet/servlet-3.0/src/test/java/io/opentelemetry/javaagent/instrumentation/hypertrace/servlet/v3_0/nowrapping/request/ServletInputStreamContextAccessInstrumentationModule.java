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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.request;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletInputStream;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.buffer.ByteBufferSpanPair;

// SPI explicitly added in META-INF/services/...
public class ServletInputStreamContextAccessInstrumentationModule extends InstrumentationModule
    implements InstrumentationModuleMuzzle {

  public ServletInputStreamContextAccessInstrumentationModule() {
    super("test-servlet-input-stream");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new InputStreamTriggerInstrumentation());
  }

  @Override
  public Map<String, ClassRef> getMuzzleReferences() {
    return Collections.emptyMap();
  }

  @Override
  public void registerMuzzleVirtualFields(VirtualFieldMappingsBuilder builder) {
    builder.register("javax.servlet.ServletInputStream", ByteBufferSpanPair.class.getName());
  }

  @Override
  public List<String> getMuzzleHelperClassNames() {
    return Collections.emptyList();
  }

  static final class InputStreamTriggerInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return named("org.ServletStreamContextAccess");
    }

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
          named("addToInputStreamContext").and(takesArguments(2)).and(isPublic()),
          ServletInputStreamContextAccessInstrumentationModule.class.getName() + "$TestAdvice");
    }
  }

  static class TestAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.Argument(0) ServletInputStream servletInputStream,
        @Advice.Argument(1) ByteBufferSpanPair metadata) {
      VirtualField<ServletInputStream, ByteBufferSpanPair> contextStore =
          VirtualField.find(ServletInputStream.class, ByteBufferSpanPair.class);
      contextStore.set(servletInputStream, metadata);
    }
  }
}
