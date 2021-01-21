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

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;

@AutoService(InstrumentationModule.class)
public class PrintWriterInstrumentationModule extends InstrumentationModule {

  public PrintWriterInstrumentationModule() {
    super("printwriter", "servlet", "servlet-3", "ht", "servlet-no-wrapping");
  }

  @Override
  protected Map<String, String> contextStore() {
    return Collections.singletonMap("java.io.PrintWriter", BoundedCharArrayWriter.class.getName());
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new PrintWriterInstrumentation());
  }
}
