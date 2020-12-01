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

package org.hypertrace.agent.otel.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.spi.ByteBuddyAgentCustomizer;
import java.security.ProtectionDomain;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;

@AutoService(ByteBuddyAgentCustomizer.class)
public class ByteBuddyIgnoreClassesCustomizer implements ByteBuddyAgentCustomizer {

  private static final String[] IGNORED_PACKAGES = {
    "io.opentracing.contrib.specialagent.", "com.newrelic.", "com.nr.agent.", "datadog.",
  };

  @Override
  public AgentBuilder customize(AgentBuilder agentBuilder) {
    return agentBuilder.ignore(
        new AgentBuilder.RawMatcher() {
          @Override
          public boolean matches(
              TypeDescription typeDescription,
              ClassLoader classLoader,
              JavaModule module,
              Class<?> classBeingRedefined,
              ProtectionDomain protectionDomain) {

            String className =
                classLoader == null
                    ? typeDescription.getTypeName()
                    : classLoader.getClass().getName();
            return matchesIgnoredClassName(IGNORED_PACKAGES, className);
          }
        });
  }

  private static boolean matchesIgnoredClassName(String[] ignoredClasses, String className) {
    if (className == null) {
      return false;
    }

    for (String ignoredClass : ignoredClasses) {
      if (className.startsWith(ignoredClass)) {
        return true;
      }
    }
    return false;
  }
}
