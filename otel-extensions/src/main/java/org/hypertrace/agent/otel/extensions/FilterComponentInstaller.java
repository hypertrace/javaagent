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
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import java.util.List;
import java.util.stream.Collectors;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.filter.FilterRegistry;
import org.hypertrace.agent.otel.extensions.config.HypertraceConfig;

@AutoService(ComponentInstaller.class)
public class FilterComponentInstaller implements ComponentInstaller {

  @Override
  public void beforeByteBuddyAgent(Config config) {
    AgentConfig agentConfig = HypertraceConfig.get();
    List<String> jarPaths =
        agentConfig.getJavaagent().getFilterJarPathsList().stream()
            .map(r -> r.getValue())
            .collect(Collectors.toList());
    // resolves filter via service loader resolution
    FilterRegistry.initialize(jarPaths);
  }

  @Override
  public void afterByteBuddyAgent(Config config) {}
}
