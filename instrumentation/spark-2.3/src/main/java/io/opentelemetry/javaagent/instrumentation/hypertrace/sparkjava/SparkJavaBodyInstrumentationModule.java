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

package io.opentelemetry.javaagent.instrumentation.hypertrace.sparkjava;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.NameMatchers.namedOneOf;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.Servlet31NoWrappingInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.Servlet31NoWrappingInstrumentationModule;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.request.BufferedReaderInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.request.ServletInputStreamInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.request.ServletRequestInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.response.ServletOutputStreamInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping.response.ServletResponseInstrumentation;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * {@code Spark.after} is not being called if a handler throws an exception. Exception handler
 * {@code Spark.exception} cannot be used because it overrides user defined exception handlers. This
 * might be fine as on exception there is usually not body send to users.
 */
@AutoService(InstrumentationModule.class)
public class SparkJavaBodyInstrumentationModule extends Servlet31NoWrappingInstrumentationModule {

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(
        new SparkJavaBodyInstrumentation(),
        new Servlet31NoWrappingInstrumentation(),
        new ServletRequestInstrumentation(),
        new ServletInputStreamInstrumentation(),
        new BufferedReaderInstrumentation(),
        new ServletResponseInstrumentation(),
        new ServletOutputStreamInstrumentation());
  }

  private static class SparkJavaBodyInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return named("spark.webserver.MatcherFilter").or(named("spark.http.matching.MatcherFilter"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      return singletonMap(
          namedOneOf("doFilter")
              .and(takesArgument(0, named("javax.servlet.ServletRequest")))
              .and(takesArgument(1, named("javax.servlet.ServletResponse")))
              .and(isPublic()),
          Servlet31NoWrappingInstrumentation.ServletAdvice.class.getName());
    }
  }
}
