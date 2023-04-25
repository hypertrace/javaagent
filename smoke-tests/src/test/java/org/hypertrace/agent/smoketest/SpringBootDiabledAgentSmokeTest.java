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

import java.io.IOException;
import okhttp3.Request;
import okhttp3.Response;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

public class SpringBootDiabledAgentSmokeTest extends AbstractSmokeTest {

  @Override
  protected String getTargetImage(int jdk) {
    return "ghcr.io/open-telemetry/java-test-containers:smoke-springboot-jdk"
        + jdk
        + "-20210218.577304949";
  }

  private GenericContainer app;

  @BeforeEach
  void beforeEach() {
    app = createAppUnderTest(8);
    // TODO: use this to disable ht-javaagent
    app.addEnv("HT_ENABLED", "false");
    app.addEnv("OTEL_JAVAAGENT_ENABLED", "false");
    app.withCopyFileToContainer(
        MountableFile.forClasspathResource("/ht-config-all-disabled.yaml"),
        "/etc/ht-config-all-disabled.yaml");
    app.withEnv("HT_CONFIG_FILE", "/etc/ht-config-all-disabled.yaml");
    app.start();
  }

  @AfterEach
  void afterEach() {
    if (app != null) {
      app.stop();
    }
  }

  @Test()
  public void get() throws IOException {
    // TODO test with multiple JDK (11, 14)
    String url = String.format("http://localhost:%d/greeting", app.getMappedPort(8080));
    Request request = new Request.Builder().url(url).get().build();

    try (Response response = client.newCall(request).execute()) {
      Assertions.assertEquals(response.body().string(), "Hi!");
    }

    Assertions.assertThrows(
        ConditionTimeoutException.class,
        () -> {
          waitForTraces();
        });
    Assertions.assertThrows(
        ConditionTimeoutException.class,
        () -> {
          waitForMetrics();
        });
  }
}
