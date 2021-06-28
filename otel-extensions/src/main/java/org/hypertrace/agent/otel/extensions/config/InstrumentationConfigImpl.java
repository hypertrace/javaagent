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
import org.hypertrace.agent.core.config.InstrumentationConfig;

@AutoService(InstrumentationConfig.class)
public class InstrumentationConfigImpl implements InstrumentationConfig {

  private final AgentConfig agentConfig = HypertraceConfig.get();

  private final Message httpHeaders;
  private final Message httpBody;
  private final Message rpcMetadata;
  private final Message rpcBody;

  public InstrumentationConfigImpl() {
    DataCapture dataCapture = agentConfig.getDataCapture();
    this.httpHeaders = new MessageImpl(dataCapture.getHttpHeaders());
    this.httpBody = new MessageImpl(dataCapture.getHttpBody());
    this.rpcMetadata = new MessageImpl(dataCapture.getRpcMetadata());
    this.rpcBody = new MessageImpl(dataCapture.getRpcBody());
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

  private static final class MessageImpl implements Message {

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
}
