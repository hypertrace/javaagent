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

import com.google.auto.service.AutoService;
import org.hypertrace.agent.config.Config;
import org.hypertrace.agent.core.config.ReportingConfig;

@AutoService(ReportingConfig.class)
public final class ReportingConfigImpl implements ReportingConfig {

  private final Opa opa;
  private final Config.Reporting reporting;

  public ReportingConfigImpl(final Config.Reporting reporting) {
    this.opa = new OpaImpl(reporting.getOpa());
    this.reporting = reporting;
  }

  @Override
  public Opa opa() {
    return opa;
  }

  @Override
  public boolean secure() {
    return reporting.getSecure().getValue();
  }

  @Override
  public String token() {
    return reporting.getToken().getValue();
  }

  private static final class OpaImpl implements Opa {

    private final Config.Opa opa;

    public OpaImpl(final Config.Opa opa) {
      this.opa = opa;
    }

    @Override
    public boolean enabled() {
      return opa.hasEnabled() ? opa.getEnabled().getValue() : true;
    }

    @Override
    public String endpoint() {
      return opa.getEndpoint().getValue();
    }

    @Override
    public int pollPeriodSeconds() {
      return opa.getPollPeriodSeconds().getValue();
    }
  }
}
