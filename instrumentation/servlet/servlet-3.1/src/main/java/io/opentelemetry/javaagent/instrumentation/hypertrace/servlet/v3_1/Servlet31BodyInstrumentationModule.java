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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.NameMatchers.namedOneOf;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * TODO https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1395 is resolved
 * move this to org.hypertrace package.
 */
@AutoService(InstrumentationModule.class)
public class Servlet31BodyInstrumentationModule extends InstrumentationModule {

  public Servlet31BodyInstrumentationModule() {
    super(Servlet31InstrumentationName.PRIMARY, Servlet31InstrumentationName.OTHER);
  }

  public Servlet31BodyInstrumentationModule(
      String mainInstrumentationName, String... otherInstrumentationNames) {
    super(mainInstrumentationName, otherInstrumentationNames);
  }

  @Override
  protected boolean defaultEnabled() {
    return false;
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
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    // Optimization for expensive typeMatcher.
    return hasClassesNamed("javax.servlet.http.HttpServlet", "javax.servlet.ReadListener");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new Servlet31BodyInstrumentation());
  }

  private static final class Servlet31BodyInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return safeHasSuperType(namedOneOf("javax.servlet.Filter", "javax.servlet.http.HttpServlet"));
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
}
