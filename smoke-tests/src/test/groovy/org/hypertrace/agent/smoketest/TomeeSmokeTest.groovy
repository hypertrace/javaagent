/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hypertrace.agent.smoketest

import spock.lang.Ignore

import java.time.Duration
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.wait.strategy.WaitStrategy

@Ignore
@AppServer(version = "7.0.0", jdk = "8")
@AppServer(version = "8.0.6", jdk = "8")
@AppServer(version = "8.0.6", jdk = "11")
class TomeeSmokeTest extends AppServerTest {

  protected String getTargetImage(String jdk, String serverVersion) {
    "hypertrace/java-agent-test-containers:tomee-${serverVersion}-jdk$jdk-20210224.596496007"
  }

  @Override
  protected WaitStrategy getWaitStrategy() {
    return Wait
      .forLogMessage(".*Server startup in.*", 1)
      .withStartupTimeout(Duration.ofMinutes(3))
  }

  @Override
  protected String getSpanName(String path) {
    switch (path) {
      case "/app/WEB-INF/web.xml":
        return "CoyoteAdapter.service"
    }
    return path
  }
}
