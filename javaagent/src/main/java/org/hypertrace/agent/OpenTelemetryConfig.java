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

package org.hypertrace.agent;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

final class OpenTelemetryConfig {
  private static final String OTEL_CONF_FILE = "otel.trace.config";
  private static final Properties LOADED_OTEL_CONF_FILE = loadConfigurationFile();

  private OpenTelemetryConfig() {}

  /** Set default value for a property in OTEL trace config. */
  static void setDefault(String property, String value) {
    if (!isConfigured(property)) {
      System.setProperty(property, value);
    }
  }

  private static boolean isConfigured(String propertyName) {
    return System.getProperty(propertyName) != null
        || System.getenv(toEnvVarName(propertyName)) != null
        || LOADED_OTEL_CONF_FILE.containsKey(propertyName);
  }

  private static String toEnvVarName(String propertyName) {
    return propertyName.toUpperCase().replaceAll("\\.", "_");
  }

  // Taken from
  // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/9523f9ffe624f14a1fba1dbd40e0f7b489b005ae/javaagent-tooling/src/main/java/io/opentelemetry/javaagent/tooling/config/ConfigInitializer.java#L51
  private static Properties loadConfigurationFile() {
    Properties properties = new Properties();

    // Reading from system property first and from env after
    String configurationFilePath = System.getProperty(OTEL_CONF_FILE);
    if (configurationFilePath == null) {
      configurationFilePath = System.getenv(toEnvVarName(OTEL_CONF_FILE));
    }
    if (configurationFilePath == null) {
      return properties;
    }

    // Normalizing tilde (~) paths for unix systems
    configurationFilePath =
        configurationFilePath.replaceFirst("^~", System.getProperty("user.home"));

    File configurationFile = new File(configurationFilePath);
    if (!configurationFile.exists()) {
      return properties;
    }

    try (FileReader fileReader = new FileReader(configurationFile)) {
      properties.load(fileReader);
    } catch (IOException ignored) {
      // OTel agent will log this error anyway
    }

    return properties;
  }
}
