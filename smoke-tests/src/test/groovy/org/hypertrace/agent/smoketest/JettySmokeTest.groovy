/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hypertrace.agent.smoketest

@AppServer(version = "9.4.35", jdk = "8")
@AppServer(version = "9.4.35", jdk = "8-openj9")
@AppServer(version = "9.4.35", jdk = "11")
@AppServer(version = "9.4.35", jdk = "11-openj9")
@AppServer(version = "10.0.0", jdk = "11")
@AppServer(version = "10.0.0", jdk = "11-openj9")
@AppServer(version = "10.0.0", jdk = "15")
@AppServer(version = "10.0.0", jdk = "15-openj9")
class JettySmokeTest extends AppServerTest {

  protected String getTargetImage(String jdk, String serverVersion) {
    "hypertrace/java-agent-test-containers:jetty-${serverVersion}-jdk$jdk-20210226.602156580"
  }

  def getJettySpanName() {
    "HandlerWrapper.handle"
  }

  @Override
  protected String getSpanName(String path) {
    switch (path) {
      case "/app/WEB-INF/web.xml":
      case "/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless":
        return getJettySpanName()
    }
    return path
  }
}
