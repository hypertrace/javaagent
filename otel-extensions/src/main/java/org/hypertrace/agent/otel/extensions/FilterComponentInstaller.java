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
import com.google.protobuf.StringValue;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.List;
import java.util.stream.Collectors;
import org.hypertrace.agent.config.v1.Config.AgentConfig;
import org.hypertrace.agent.filter.FilterRegistry;
import org.hypertrace.agent.filter.spi.FilterProviderConfig;
import org.hypertrace.agent.otel.extensions.config.HypertraceConfig;

@AutoService(BeforeAgentListener.class)
public class FilterComponentInstaller implements BeforeAgentListener {

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    FilterProviderConfig providerConfig = new FilterProviderConfig();
    AgentConfig agentConfig = HypertraceConfig.get();
    String serviceName = agentConfig.getServiceName().getValue();
    providerConfig.setServiceName(serviceName);
    List<String> jarPaths =
        agentConfig.getJavaagent().getFilterJarPathsList().stream()
            .map(StringValue::getValue)
            .collect(Collectors.toList());
    // resolves filter via service loader resolution
    FilterRegistry.initialize(providerConfig, jarPaths, getClass().getClassLoader());
  }

  @Override
  public int order() {
    // Configs should be loaded before FilterComponents because filters use configs
    return 1;
  }
}
