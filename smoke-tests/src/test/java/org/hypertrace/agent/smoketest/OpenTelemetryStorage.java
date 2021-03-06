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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class OpenTelemetryStorage extends GenericContainer<OpenTelemetryStorage> {
  public static final int PORT = 8080;

  public OpenTelemetryStorage(String dockerImage) {
    super(DockerImageName.parse(dockerImage));
    init();
  }

  private void init() {
    withExposedPorts(PORT).waitingFor(Wait.forHttp("/health").forPort(PORT));
  }

  /** @return public port to query traces */
  public int getPort() {
    return getMappedPort(PORT);
  }
}
