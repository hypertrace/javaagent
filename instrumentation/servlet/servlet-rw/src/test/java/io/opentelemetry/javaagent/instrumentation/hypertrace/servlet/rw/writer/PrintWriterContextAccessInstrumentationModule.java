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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.rw.writer;

import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.instrumentation.api.field.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;

// SPI explicitly added in META-INF/services/...
public class PrintWriterContextAccessInstrumentationModule extends InstrumentationModule {

  public PrintWriterContextAccessInstrumentationModule() {
    super("test-print-writer");
  }

  @Override
  public Map<String, String> getMuzzleContextStoreClasses() {
    return Collections.singletonMap("java.io.PrintWriter", BoundedCharArrayWriter.class.getName());
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new PrintWriterTriggerInstrumentation());
  }

  class PrintWriterTriggerInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return named("org.BufferedReaderPrintWriterContextAccess");
    }

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
          named("addToPrintWriterContext").and(takesArguments(2)).and(isPublic()),
          PrintWriterContextAccessInstrumentationModule.class.getName() + "$TestAdvice");
    }
  }

  static class TestAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.Argument(0) PrintWriter printWriter,
        @Advice.Argument(1) BoundedCharArrayWriter metadata) {
      VirtualField<PrintWriter, BoundedCharArrayWriter> contextStore =
          VirtualField.find(PrintWriter.class, BoundedCharArrayWriter.class);
      contextStore.set(printWriter, metadata);
    }
  }
}
