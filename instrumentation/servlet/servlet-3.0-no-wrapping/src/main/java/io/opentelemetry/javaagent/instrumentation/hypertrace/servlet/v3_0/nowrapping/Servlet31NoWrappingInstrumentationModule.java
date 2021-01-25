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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.ClassLoaderMatcher.hasClassesNamed;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.request.RequestStreamReaderHolder;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.request.ServletInputStreamInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.request.ServletRequestInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.response.ServletOutputStreamInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.response.ServletResponseInstrumentation;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;
import org.hypertrace.agent.core.instrumentation.buffer.ByteBufferSpanPair;
import org.hypertrace.agent.core.instrumentation.buffer.CharBufferSpanPair;

@AutoService(InstrumentationModule.class)
public class Servlet31NoWrappingInstrumentationModule extends InstrumentationModule {

  public Servlet31NoWrappingInstrumentationModule() {
    super(Servlet31InstrumentationName.PRIMARY, Servlet31InstrumentationName.OTHER);
  }

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("javax.servlet.http.HttpServlet");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(
        new Servlet31NoWrappingInstrumentation(),
        new ServletRequestInstrumentation(),
        new ServletInputStreamInstrumentation(),
        new ServletResponseInstrumentation(),
        new ServletOutputStreamInstrumentation());
  }

  @Override
  protected Map<String, String> contextStore() {
    Map<String, String> context = new HashMap<>();
    // capture request body
    context.put("javax.servlet.http.HttpServletRequest", RequestStreamReaderHolder.class.getName());
    context.put("javax.servlet.ServletInputStream", ByteBufferSpanPair.class.getName());
    context.put("java.io.BufferedReader", CharBufferSpanPair.class.getName());

    // capture response body
    context.put("javax.servlet.ServletOutputStream", BoundedByteArrayOutputStream.class.getName());
    context.put("java.io.PrintWriter", BoundedCharArrayWriter.class.getName());
    return context;
  }
}
