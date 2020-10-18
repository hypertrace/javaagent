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

package io.opentelemetry.instrumentation.hypertrace.servlet.v3_0;

public class DynamicConfig {

  private DynamicConfig() {}

  public static boolean isEnabled(String[] instrumentationNames) {
    String integrationEnabled = getProperty("hypertrace.integration.all.enabled");
    if (integrationEnabled != null && "false".equals(integrationEnabled.toLowerCase())) {
      return false;
    }

    for (String name : instrumentationNames) {
      integrationEnabled = getProperty("hypertrace.integration." + name + ".enabled");
      if (integrationEnabled != null && "false".equals(integrationEnabled.toLowerCase())) {
        return false;
      }
    }
    return true;
  }

  public static String getProperty(String name) {
    return System.getenv(name);
  }
}
