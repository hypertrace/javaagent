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

package org.hypertrace.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.config.Config.DataCapture;
import org.hypertrace.agent.config.Config.Message;
import org.hypertrace.agent.config.Config.Reporting;

/** {@link HypertraceConfig} loads a yaml config from file. */
public class HypertraceConfig {

  private HypertraceConfig() {}

  private static AgentConfig agentConfig;

  static final String DEFAULT_REPORTING_ADDRESS = "http://localhost:9411/api/v2/spans";
  static final String DEFAULT_SERVICE_NAME = "default_service_name";

  public static AgentConfig get() {
    if (agentConfig == null) {
      synchronized (HypertraceConfig.class) {
        if (agentConfig == null) {
          try {
            agentConfig = load();
          } catch (IOException e) {
            throw new RuntimeException("Could not load config", e);
          }
        }
      }
    }
    return agentConfig;
  }

  /** Reset the config, use only in tests. */
  public static void reset() {
    agentConfig = null;
  }

  private static AgentConfig load() throws IOException {
    String configFile = EnvironmentConfig.getProperty(EnvironmentConfig.CONFIG_FILE_PROPERTY);
    if (configFile == null) {
      return EnvironmentConfig.applyPropertiesAndEnvVars(applyDefaults(AgentConfig.newBuilder()))
          .build();
    }
    return load(configFile);
  }

  @VisibleForTesting
  static AgentConfig load(String filename) throws IOException {
    File configFile = new File(filename);
    if (!configFile.exists() || configFile.isDirectory() || !configFile.canRead()) {
      throw new IllegalArgumentException(
          String.format("Config file %s either does not exist or cannot be read", configFile));
    }

    InputStream fileInputStream = new FileInputStream(configFile);
    String json = convertYamlToJson(fileInputStream);

    AgentConfig.Builder configBuilder = AgentConfig.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(json, configBuilder);

    return EnvironmentConfig.applyPropertiesAndEnvVars(applyDefaults(configBuilder)).build();
  }

  private static AgentConfig.Builder applyDefaults(AgentConfig.Builder configBuilder) {
    if (configBuilder.getServiceName().isEmpty()) {
      configBuilder.setServiceName(DEFAULT_SERVICE_NAME);
    }

    Reporting.Builder reportingBuilder =
        applyReportingDefaults(configBuilder.getReporting().toBuilder());
    configBuilder.setReporting(reportingBuilder);

    DataCapture.Builder dataCaptureBuilder =
        setDefaultsToDataCapture(configBuilder.getDataCapture().toBuilder());
    configBuilder.setDataCapture(dataCaptureBuilder);
    return configBuilder;
  }

  private static Reporting.Builder applyReportingDefaults(Reporting.Builder builder) {
    if (!builder.hasAddress()) {
      builder.setAddress(StringValue.newBuilder().setValue(DEFAULT_REPORTING_ADDRESS).build());
    }
    return builder;
  }

  private static DataCapture.Builder setDefaultsToDataCapture(DataCapture.Builder builder) {
    builder.setHttpHeaders(applyMessageDefaults(builder.getHttpHeaders().toBuilder()));
    builder.setHttpBody(applyMessageDefaults(builder.getHttpBody().toBuilder()));
    builder.setRpcMetadata(applyMessageDefaults(builder.getRpcMetadata().toBuilder()));
    builder.setRpcBody(applyMessageDefaults(builder.getRpcBody().toBuilder()));
    return builder;
  }

  private static Message.Builder applyMessageDefaults(Message.Builder builder) {
    if (!builder.hasRequest()) {
      builder.setRequest(BoolValue.newBuilder().setValue(true).build());
    }
    if (!builder.hasResponse()) {
      builder.setResponse(BoolValue.newBuilder().setValue(true).build());
    }
    return builder;
  }

  private static String convertYamlToJson(InputStream yaml) throws IOException {
    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
    Object obj = yamlReader.readValue(yaml, Object.class);

    ObjectMapper jsonWriter = new ObjectMapper();
    return jsonWriter.writeValueAsString(obj);
  }
}
