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
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.config.Config.DataCapture;
import org.hypertrace.agent.config.Config.Message;
import org.hypertrace.agent.config.Config.Opa;
import org.hypertrace.agent.config.Config.Opa.Builder;
import org.hypertrace.agent.config.Config.PropagationFormat;
import org.hypertrace.agent.config.Config.Reporting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HypertraceConfig} loads a yaml config from file. */
public class HypertraceConfig {

  private HypertraceConfig() {}

  private static final Logger log = LoggerFactory.getLogger(HypertraceConfig.class);

  private static AgentConfig agentConfig;

  static final String DEFAULT_SERVICE_NAME = "unknown";
  static final String DEFAULT_REPORTING_ADDRESS = "http://localhost:9411/api/v2/spans";
  static final String DEFAULT_OPA_ADDRESS = "http://opa.traceableai:8181/";
  static final int DEFAULT_OPA_POLL_PERIOD_SECONDS = 30;

  public static AgentConfig get() {
    if (agentConfig == null) {
      synchronized (HypertraceConfig.class) {
        if (agentConfig == null) {
          try {
            agentConfig = load();
            log.info(
                "Config loaded: {}",
                JsonFormat.printer().omittingInsignificantWhitespace().print(agentConfig));
          } catch (IOException e) {
            throw new RuntimeException("Could not load config", e);
          }
        }
      }
    }
    return agentConfig;
  }

  public static boolean isInstrumentationEnabled(String primaryName, String[] otherNames) {
    // the instNames is not used because the config does not support it at the moment.

    AgentConfig agentConfig = get();
    // disabled if all is disabled
    if (!agentConfig.getDataCapture().getHttpBody().getRequest().getValue()
        && !agentConfig.getDataCapture().getHttpBody().getResponse().getValue()
        && !agentConfig.getDataCapture().getHttpHeaders().getRequest().getValue()
        && !agentConfig.getDataCapture().getHttpHeaders().getResponse().getValue()
        && !agentConfig.getDataCapture().getRpcMetadata().getRequest().getValue()
        && !agentConfig.getDataCapture().getRpcMetadata().getResponse().getValue()) {
      return false;
    }
    return true;
  }

  /** Reset the config, use only in tests. */
  @VisibleForTesting
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
      log.error("Config file {} does not exist", filename);
      return AgentConfig.newBuilder().build();
    }

    AgentConfig.Builder configBuilder = AgentConfig.newBuilder();
    Parser jsonParser = JsonFormat.parser().ignoringUnknownFields();
    try (InputStream fileInputStream = new FileInputStream(configFile)) {
      if (filename.toLowerCase().endsWith("json")) {
        Reader targetReader = new InputStreamReader(fileInputStream);
        jsonParser.merge(targetReader, configBuilder);
      } else {
        String json = convertYamlToJson(fileInputStream);
        jsonParser.merge(json, configBuilder);
      }
      return EnvironmentConfig.applyPropertiesAndEnvVars(applyDefaults(configBuilder)).build();
    }
  }

  private static AgentConfig.Builder applyDefaults(AgentConfig.Builder configBuilder) {
    if (configBuilder.getServiceName().getValue().isEmpty()) {
      configBuilder.setServiceName(StringValue.newBuilder().setValue(DEFAULT_SERVICE_NAME).build());
    }

    Reporting.Builder reportingBuilder =
        applyReportingDefaults(configBuilder.getReporting().toBuilder());
    configBuilder.setReporting(reportingBuilder);

    DataCapture.Builder dataCaptureBuilder =
        setDefaultsToDataCapture(configBuilder.getDataCapture().toBuilder());
    configBuilder.setDataCapture(dataCaptureBuilder);

    if (configBuilder.getPropagationFormatsList().isEmpty()) {
      configBuilder.addPropagationFormats(PropagationFormat.TRACE_CONTEXT);
    }
    return configBuilder;
  }

  private static Reporting.Builder applyReportingDefaults(Reporting.Builder builder) {
    if (!builder.hasAddress()) {
      builder.setAddress(StringValue.newBuilder().setValue(DEFAULT_REPORTING_ADDRESS).build());
    }
    Builder opaBuilder = applyOpaDefaults(builder.getOpa().toBuilder());
    builder.setOpa(opaBuilder);
    return builder;
  }

  private static Opa.Builder applyOpaDefaults(Opa.Builder builder) {
    if (!builder.hasAddress()) {
      builder.setAddress(StringValue.newBuilder().setValue(DEFAULT_OPA_ADDRESS).build());
    }
    if (!builder.hasPollPeriod()) {
      builder.setPollPeriod(
          Int32Value.newBuilder().setValue(DEFAULT_OPA_POLL_PERIOD_SECONDS).build());
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
