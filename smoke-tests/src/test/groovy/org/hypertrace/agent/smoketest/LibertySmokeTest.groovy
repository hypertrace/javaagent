/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hypertrace.agent.smoketest

import java.time.Duration
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.wait.strategy.WaitStrategy

@AppServer(version = "20.0.0.12", jdk = "8")
@AppServer(version = "20.0.0.12", jdk = "8-openj9")
@AppServer(version = "20.0.0.12", jdk = "11")
@AppServer(version = "20.0.0.12", jdk = "11-openj9")
class LibertySmokeTest extends AppServerTest {

  protected String getTargetImage(String jdk, String serverVersion) {
    "hypertrace/java-agent-test-containers:liberty-${serverVersion}-jdk$jdk-20210226.602156580"
  }

  @Override
  protected WaitStrategy getWaitStrategy() {
    return Wait
            .forLogMessage(".*server is ready to run a smarter planet.*", 1)
            .withStartupTimeout(Duration.ofMinutes(3))
  }

  @Override
  protected String getSpanName(String path) {
    switch (path) {
      case "/app/hello.txt":
      case "/app/file-that-does-not-exist":
        return "/app/*"
    }
    return super.getSpanName(path)
  }
}
