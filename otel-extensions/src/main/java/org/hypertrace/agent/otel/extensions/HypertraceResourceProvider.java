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
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.bootstrap.config.HypertraceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(ResourceProvider.class)
public class HypertraceResourceProvider implements ResourceProvider {

  private static final Logger log =
      LoggerFactory.getLogger(HypertraceResourceProvider.class.getName());

  private final CgroupsReader cgroupsReader = new CgroupsReader();
  private final AgentConfig agentConfig = HypertraceConfig.get();

  @Override
  public Resource createResource(ConfigProperties config) {
    AttributesBuilder builder = Attributes.builder();
    String containerId = this.cgroupsReader.readContainerId();
    if (containerId != null && !containerId.isEmpty()) {
      builder.put(ResourceAttributes.CONTAINER_ID, containerId);
    }
    builder.put(ResourceAttributes.SERVICE_NAME, agentConfig.getServiceName().getValue());
    builder.put(ResourceAttributes.TELEMETRY_SDK_NAME, "hypertrace");
    builder.put(ResourceAttributes.TELEMETRY_SDK_LANGUAGE, "java");
    String agentVersion = getAgentVersion();
    builder.put(ResourceAttributes.TELEMETRY_SDK_VERSION, agentVersion);
    builder.put(ResourceAttributes.TELEMETRY_AUTO_VERSION, agentVersion);
    return Resource.create(builder.build());
  }

  private String getAgentVersion() {
    String agentVersion = "";
    try {
      Class<?> hypertraceAgentClass =
          Class.forName("org.hypertrace.agent.instrument.HypertraceAgent", true, null);
      agentVersion = hypertraceAgentClass.getPackage().getImplementationVersion();
    } catch (ClassNotFoundException e) {
      log.warn("Could not load HypertraceAgent class");
    }
    return agentVersion;
  }
}
