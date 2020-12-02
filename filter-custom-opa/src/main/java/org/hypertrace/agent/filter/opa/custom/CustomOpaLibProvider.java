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

package org.hypertrace.agent.filter.opa.custom;

import com.google.auto.service.AutoService;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.config.Config.Reporting;
import org.hypertrace.agent.core.EnvironmentConfig;
import org.hypertrace.agent.core.HypertraceConfig;
import org.hypertrace.agent.filter.FilterRegistry;
import org.hypertrace.agent.filter.api.Filter;
import org.hypertrace.agent.filter.spi.FilterProvider;

@AutoService(FilterProvider.class)
public class CustomOpaLibProvider implements FilterProvider {

  public CustomOpaLibProvider() {
    String property = FilterRegistry.getProviderDisabledPropertyName(CustomOpaLibProvider.class);
    // by default disable this provider until HT agent config includes OPA
    if (EnvironmentConfig.getProperty(property) == null) {
      System.setProperty(property, "true");
    }
  }

  @Override
  public Filter create() {
    AgentConfig agentConfig = HypertraceConfig.get();
    Reporting reporting = agentConfig.getReporting();
    return new CustomOpaLib(
        reporting.getOpa().getEndpoint().getValue(),
        reporting.getToken().getValue(),
        reporting.getSecure().getValue(),
        reporting.getOpa().getPollPeriodSeconds().getValue());
  }
}
