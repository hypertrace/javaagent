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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v2_2.nowrapping;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v2_2.nowrapping.request.ServletInputStreamInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v2_2.nowrapping.request.ServletRequestInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v2_2.nowrapping.response.ServletOutputStreamInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v2_2.nowrapping.response.ServletResponseInstrumentation;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
@AutoService(InstrumentationModule.class)
public class Servlet2InstrumentationModule extends InstrumentationModule {

  public Servlet2InstrumentationModule() {
    super(Servlet2InstrumentationName.PRIMARY, Servlet2InstrumentationName.OTHER);
  }

  @Override
  public int order() {
    return 1;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(
        new Servlet2AndFilterInstrumentation(),
        new ServletRequestInstrumentation(),
        new ServletInputStreamInstrumentation(),
        new ServletResponseInstrumentation(),
        new ServletOutputStreamInstrumentation());
  }
}
