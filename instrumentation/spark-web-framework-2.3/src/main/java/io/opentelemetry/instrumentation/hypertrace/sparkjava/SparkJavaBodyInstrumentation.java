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

package io.opentelemetry.instrumentation.hypertrace.sparkjava;

import static io.opentelemetry.javaagent.tooling.matcher.NameMatchers.namedOneOf;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.Servlet31Advice;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * {@code Spark.after} is not being called if a handler throws an exception. Exception handler
 * {@code Spark.exception} cannot be used because it overrides user defined exception handlers. This
 * might be fine as on exception there is usually not body send to users.
 */
@AutoService(Instrumenter.class)
public class SparkJavaBodyInstrumentation extends Instrumenter.Default {

  public SparkJavaBodyInstrumentation() {
    super(InstrumentationName.INSTRUMENTATION_NAME[0], InstrumentationName.INSTRUMENTATION_NAME[1]);
  }

  @Override
  public int getOrder() {
    return 1;
  }

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
        Servlet31Advice.class.getName());
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "org.hypertrace.agent.filter.FilterProvider",
      "org.hypertrace.agent.filter.FilterEvaluator",
      "org.hypertrace.agent.filter.FilterResult",
      "org.hypertrace.agent.filter.ExecutionBlocked",
      "org.hypertrace.agent.filter.ExecutionNotBlocked",
      "org.hypertrace.agent.filter.MockFilterEvaluator",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.ByteBufferData",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.CharBufferData",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.BufferedWriterWrapper",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.BufferedReaderWrapper",
      "io.opentelemetry.instrumentation.hypertrace.servlet.common.ServletSpanDecorator",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletResponse",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletResponse$BufferingServletOutputStream",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletRequest",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.BufferingHttpServletRequest$ServletInputStreamWrapper",
      "io.opentelemetry.instrumentation.hypertrace.servlet.v3_1.Servlet31Advice",
      // TODO Instrumentation name is not used in the advice method to check whether
      // instrumentation is enabled or not
      packageName + ".InstrumentationName",
    };
  }
}
