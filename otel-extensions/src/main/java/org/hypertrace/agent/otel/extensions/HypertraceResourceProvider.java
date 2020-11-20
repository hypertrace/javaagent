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
import io.opentelemetry.api.common.Attributes.Builder;
import io.opentelemetry.sdk.resources.ResourceAttributes;
import io.opentelemetry.sdk.resources.ResourceProvider;

@AutoService(ResourceProvider.class)
public class HypertraceResourceProvider extends ResourceProvider {

  private final CgroupsReader cgroupsReader = new CgroupsReader();

  @Override
  protected Attributes getAttributes() {
    Builder builder = Attributes.builder();
    String containerId = this.cgroupsReader.readContainerId();
    if (containerId != null && !containerId.isEmpty()) {
      builder.put(ResourceAttributes.CONTAINER_ID.getKey(), containerId);
    }
    return builder.build();
  }
}
