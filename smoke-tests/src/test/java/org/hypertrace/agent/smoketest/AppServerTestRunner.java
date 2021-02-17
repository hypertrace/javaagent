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
import java.util.HashMap;
import java.util.Map;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.spockframework.runtime.Sputnik;

/**
 * Customized spock test runner that runs tests on multiple app server versions based on {@link
 * AppServer} annotations. This runner selects first server based on {@link AppServer} annotation
 * calls setupSpec, all test method and cleanupSpec, selects next {@link AppServer} and calls the
 * same methods. This process is repeated until tests have run for all {@link AppServer}
 * annotations. Tests should start server in setupSpec and stop it in cleanupSpec.
 */
public class AppServerTestRunner extends Sputnik {
  private static final Map<Class<?>, AppServer> runningAppServer =
      Collections.synchronizedMap(new HashMap<>());
  private final Class<?> testClass;
  private final AppServer[] appServers;

  public AppServerTestRunner(Class<?> clazz) throws InitializationError {
    super(clazz);
    testClass = clazz;
    appServers = clazz.getAnnotationsByType(AppServer.class);
    if (appServers.length == 0) {
      throw new IllegalStateException("Add AppServer or AppServers annotation to test class");
    }
  }

  @Override
  public void run(RunNotifier notifier) {
    // run tests for all app servers
    try {
      for (AppServer appServer : appServers) {
        runningAppServer.put(testClass, appServer);
        super.run(notifier);
      }
    } finally {
      runningAppServer.remove(testClass);
    }
  }

  // expose currently running app server
  // used to get current server and jvm version inside the test class
  public static AppServer currentAppServer(Class<?> testClass) {
    AppServer appServer = runningAppServer.get(testClass);
    if (appServer == null) {
      throw new IllegalStateException("Test not running for " + testClass);
    }
    return appServer;
  }
}
