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

package org.hypertrace.agent.otel.extensions.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.BoolValue;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.hypertrace.agent.config.Config.Opa;
import org.hypertrace.agent.config.Config.Reporting;
import org.hypertrace.agent.config.Config.Reporting.Builder;
import org.hypertrace.agent.core.config.ReportingConfig;
import org.junit.jupiter.api.Test;

final class ReportingConfigImplTest {

  @Test
  void instantiateViaServiceLoaderApi() {
    final ServiceLoader<ReportingConfig> reportingConfigs =
        ServiceLoader.load(ReportingConfig.class);
    final Iterator<ReportingConfig> iterator = reportingConfigs.iterator();
    assertTrue(iterator.hasNext());
    final ReportingConfig reportingConfig = iterator.next();
    assertNotNull(reportingConfig);
    assertFalse(iterator.hasNext());
  }

  @Test
  void opaEnabledIfNotProvided() {
    final ReportingConfigImpl reportingConfig =
        new ReportingConfigImpl(Reporting.getDefaultInstance());
    assertTrue(reportingConfig.opa().enabled());
  }

  @Test
  void opaDisabledIfExplicitlySet() {
    final Builder reportingBuilder = Reporting.getDefaultInstance().toBuilder();
    final Opa explicitOpaConfig =
        reportingBuilder.getOpaBuilder().setEnabled(BoolValue.of(false)).build();
    final ReportingConfigImpl reportingConfig =
        new ReportingConfigImpl(reportingBuilder.setOpa(explicitOpaConfig).build());
    assertFalse(reportingConfig.opa().enabled());
  }
}
