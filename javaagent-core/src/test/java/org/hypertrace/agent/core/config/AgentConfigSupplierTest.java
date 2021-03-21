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

package org.hypertrace.agent.core.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.hypertrace.agent.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class AgentConfigSupplierTest {

  private CountingAgentConfigSupplier inner;
  private AgentConfigSupplier agentConfigSupplier;

  @BeforeEach
  void beforeEach() {
    // GIVEN a supplier of AgentConfig that also counts how many time its been invoked
    inner = new CountingAgentConfigSupplier();
    // WHEN we decorate it with the AgentConfigSupplier
    agentConfigSupplier = new AgentConfigSupplier(inner);
    // VERIFY starts out reporting its been invoked 0 times
    assertEquals(0, inner.getCount());
  }

  @Test
  void testMemoizingAgentConfig() {
    // WHEN we access the config
    final Config.AgentConfig agentConfig = agentConfigSupplier.get();
    // VERIFY it has been invoked once
    assertEquals(1, inner.getCount());

    // WHEN we access the config
    final Config.AgentConfig secondConfig = agentConfigSupplier.get();
    // VERIFY the inner supplier has still only been invoked once
    assertEquals(1, inner.getCount());
    // VERIFY the two configs are the same instance
    assertSame(agentConfig, secondConfig);
  }

  @Test
  void testResetAgentConfig() {
    // WHEN we access the config
    final Config.AgentConfig agentConfig = agentConfigSupplier.get();
    // VERIFY it has been invoked once
    assertEquals(1, inner.getCount());

    // WHEN we reset the supplier
    agentConfigSupplier.reset();
    // AND WHEN we access the config again
    final Config.AgentConfig secondConfig = agentConfigSupplier.get();

    // VERIFY it has been invoked twice
    assertEquals(2, inner.getCount());
    // VERIFY the two configs are different instances
    assertNotSame(agentConfig, secondConfig);
  }

  /**
   * thread-safe {@link Config.AgentConfig} {@link Supplier} that also counts how many times it has
   * been invoked
   */
  private static final class CountingAgentConfigSupplier implements Supplier<Config.AgentConfig> {

    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public Config.AgentConfig get() {
      count.getAndIncrement();
      URL resource = getClass().getClassLoader().getResource("emptyconfig.yaml");
      try {
        return HypertraceConfig.load(resource.getPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    int getCount() {
      return count.get();
    }
  }
}
