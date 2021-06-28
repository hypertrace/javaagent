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
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.config.Config.DataCapture;
import org.hypertrace.agent.config.Config.Message;
import org.hypertrace.agent.config.Config.Opa;
import org.hypertrace.agent.config.Config.Reporting;
import org.hypertrace.agent.core.config.InstrumentationConfig;

@AutoService(InstrumentationConfig.class)
public class InstrumentationConfigImpl implements InstrumentationConfig {

  private final AgentConfig agentConfig = HypertraceConfig.get();

  private final Message httpHeaders;
  private final Message httpBody;
  private final Message rpcMetadata;
  private final Message rpcBody;
  private final Reporting reporting;

  public InstrumentationConfigImpl() {
    DataCapture dataCapture = agentConfig.getDataCapture();
    this.httpHeaders = new MessageImpl(dataCapture.getHttpHeaders());
    this.httpBody = new MessageImpl(dataCapture.getHttpBody());
    this.rpcMetadata = new MessageImpl(dataCapture.getRpcMetadata());
    this.rpcBody = new MessageImpl(dataCapture.getRpcBody());
    reporting = new ReportingImpl(agentConfig.getReporting());
  }

  @Override
  public int maxBodySizeBytes() {
    return agentConfig.getDataCapture().getBodyMaxSizeBytes().getValue();
  }

  @Override
  public Message httpHeaders() {
    return this.httpHeaders;
  }

  @Override
  public Message httpBody() {
    return this.httpBody;
  }

  @Override
  public Message rpcMetadata() {
    return this.rpcMetadata;
  }

  @Override
  public Message rpcBody() {
    return this.rpcBody;
  }

  @Override
  public Reporting reporting() {
    return reporting;
  }

  private class MessageImpl implements Message {

    private final Config.Message message;

    public MessageImpl(Config.Message message) {
      this.message = message;
    }

    @Override
    public boolean request() {
      return message.getRequest().getValue();
    }

    @Override
    public boolean response() {
      return message.getResponse().getValue();
    }
  }

  private static final class ReportingImpl implements Reporting {

    private final Opa opa;
    private final Config.Reporting reporting;

    public ReportingImpl(final Config.Reporting reporting) {
      opa = new OpaImpl(reporting.getOpa());
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
  }

  private static final class OpaImpl implements Opa {

    private final Config.Opa opa;

    public OpaImpl(final Config.Opa opa) {
      this.opa = opa;
    }

    @Override
    public boolean enabled() {
      return opa.getEnabled().getValue();
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
