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

package org.hypertrace.agent.smoketest;

import java.util.Collections;
import java.util.Set;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public class OpenTelemetryCollector extends GenericContainer<OpenTelemetryCollector> {

  public static final int JAEGER_COLLECTOR_THRIFT_PORT = 14268;
  public static final int JAEGER_COLLECTOR_GRPC_PORT   = 14250;
  public static final int HTTP_REVERSE_PROXY_PORT      = 5442;
  public static final int HTTPS_REVERSE_PROXY_PORT     = 5443;
  public static final int HEALTH_CHECK_PORT            = 13133;

  public OpenTelemetryCollector(String dockerImage) {
    super(DockerImageName.parse(dockerImage));
    init();
  }

  protected void init() {
    waitingFor(new BoundPortHttpWaitStrategy(HEALTH_CHECK_PORT));
    withExposedPorts(
        HEALTH_CHECK_PORT,
        JAEGER_COLLECTOR_THRIFT_PORT,
        JAEGER_COLLECTOR_GRPC_PORT,
        HTTP_REVERSE_PROXY_PORT,
        HTTPS_REVERSE_PROXY_PORT
        );
  }

  public static class BoundPortHttpWaitStrategy extends HttpWaitStrategy {
    private final int port;

    public BoundPortHttpWaitStrategy(int port) {
      this.port = port;
    }

    @Override
    protected Set<Integer> getLivenessCheckPorts() {
      int mapptedPort = this.waitStrategyTarget.getMappedPort(port);
      return Collections.singleton(mapptedPort);
    }
  }
}
