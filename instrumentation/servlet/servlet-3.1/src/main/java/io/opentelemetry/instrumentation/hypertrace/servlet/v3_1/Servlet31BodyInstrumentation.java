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

package io.opentelemetry.instrumentation.hypertrace.servlet.v3_1;

import static io.opentelemetry.javaagent.tooling.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static io.opentelemetry.javaagent.tooling.matcher.NameMatchers.namedOneOf;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * TODO https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1395 is resolved
 * move this to org.hypertrace package.
 */
@AutoService(Instrumenter.class)
public class Servlet31BodyInstrumentation extends Instrumenter.Default {

  public Servlet31BodyInstrumentation() {
    super(InstrumentationName.INSTRUMENTATION_NAME[0], InstrumentationName.INSTRUMENTATION_NAME[1]);
  }

  @Override
  public int getOrder() {
    /**
     * Order 1 assures that this instrumentation runs after OTEL servlet instrumentation so we can
     * access current span in our advice.
     */
    return 1;
  }

  @Override
  public ElementMatcher<ClassLoader> classLoaderMatcher() {
    // Optimization for expensive typeMatcher.
    return hasClassesNamed("javax.servlet.http.HttpServlet", "javax.servlet.ReadListener");
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(
        namedOneOf("javax.servlet.FilterChain", "javax.servlet.http.HttpServlet"));
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "io.opentelemetry.instrumentation.servlet.HttpServletRequestGetter",
      "io.opentelemetry.instrumentation.servlet.ServletHttpServerTracer",
      "io.opentelemetry.javaagent.instrumentation.servlet.v3_0.Servlet3HttpServerTracer",
      // TODO Add these to bootstrap classloader so they don't have to referenced in every
      // instrumentation, see https://github.com/hypertrace/javaagent/issues/17
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
      packageName + ".InstrumentationName",
      packageName + ".BufferingHttpServletResponse",
      packageName + ".BufferingHttpServletResponse$BufferingServletOutputStream",
      packageName + ".BufferingHttpServletRequest",
      packageName + ".BufferingHttpServletRequest$ServletInputStreamWrapper",
      packageName + ".BodyCaptureAsyncListener",
      packageName + ".Servlet31Advice",
    };
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        namedOneOf("doFilter", "service")
            .and(takesArgument(0, named("javax.servlet.ServletRequest")))
            .and(takesArgument(1, named("javax.servlet.ServletResponse")))
            .and(isPublic()),
        Servlet31Advice.class.getName());
  }
}
