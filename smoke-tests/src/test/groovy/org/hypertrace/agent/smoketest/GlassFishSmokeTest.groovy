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
@AppServer(version = "5.2020.6", jdk = "8")
@AppServer(version = "5.2020.6", jdk = "8-openj9")
@AppServer(version = "5.2020.6", jdk = "11")
@AppServer(version = "5.2020.6", jdk = "11-openj9")
class GlassFishSmokeTest extends AppServerTest {

    protected String getTargetImage(String jdk, String serverVersion) {
        "hypertrace/java-agent-test-containers:payara-${serverVersion}-jdk$jdk-20210226.602156580"
    }

    @Override
    protected Map<String, String> getExtraEnv() {
        return ["HZ_PHONE_HOME_ENABLED": "false"]
    }

    @Override
    protected WaitStrategy getWaitStrategy() {
        return Wait
                .forLogMessage(".*app was successfully deployed.*", 1)
                .withStartupTimeout(Duration.ofMinutes(3))
    }

    @Override
    protected String getSpanName(String path) {
        switch (path) {
            case "/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless":
                return "GET /*"
        }
        return super.getSpanName(path)
    }

    @Override
    boolean testRequestWebInfWebXml() {
        false
    }

}
