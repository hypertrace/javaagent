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

package org.hypertrace.agent.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hypertrace.agent.config.Config;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.config.Config.DataCapture;
import org.hypertrace.agent.config.Config.Message;
import org.hypertrace.agent.config.Config.Opa;
import org.hypertrace.agent.config.Config.Opa.Builder;
import org.hypertrace.agent.config.Config.PropagationFormat;
import org.hypertrace.agent.config.Config.Reporting;
import org.hypertrace.agent.config.ConfigurationServiceGrpc;
import org.hypertrace.agent.config.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link HypertraceConfig} loads a yaml config from file and gets dynamic config if dynamic config
 * service url is set in env vars or system properties.
 */
public class HypertraceConfig {

  private HypertraceConfig() {}

  private static final Logger log = LoggerFactory.getLogger(HypertraceConfig.class);

  // we could use a Set<Framework> but that would need to be synchronized
  // so avoiding for perf reasons
  private static volatile boolean servletCausingException;

  // volatile field in order to properly handle lazy initialization with double-checked locking
  private static volatile AgentConfig agentConfig;

  static final String DEFAULT_SERVICE_NAME = "unknown";
  static final String DEFAULT_REPORTING_ENDPOINT = "http://localhost:9411/api/v2/spans";
  static final String DEFAULT_OPA_ENDPOINT = "http://opa.traceableai:8181/";
  static final int DEFAULT_OPA_POLL_PERIOD_SECONDS = 30;
  // 128 KiB
  static final int DEFAULT_BODY_MAX_SIZE_BYTES = 128 * 1024;

  public static AgentConfig get() {
    if (agentConfig == null) {
      String dynamicConfigServiceUrl =
          EnvironmentConfig.getProperty(EnvironmentConfig.DYNAMIC_CONFIG_SERVICE_URL);
      DynamicConfigFetcher dynamicConfigFetcher = null;
      if (dynamicConfigServiceUrl != null) {
        dynamicConfigFetcher = new DynamicConfigFetcher(dynamicConfigServiceUrl);
        Executors.newScheduledThreadPool(
                1,
                runnable -> {
                  Thread thread = new Thread(runnable, "dynamic_agent_config_fetcher");
                  thread.setDaemon(true);
                  //                  thread.setContextClassLoader(null);
                  return thread;
                })
            .scheduleAtFixedRate(dynamicConfigFetcher, 60, 30, TimeUnit.SECONDS);
      }
      synchronized (HypertraceConfig.class) {
        if (agentConfig == null) {
          try {
            agentConfig = load(dynamicConfigFetcher);
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

  public static boolean disableServletWrapperTypes() {
    return servletCausingException;
  }

  /** Record any exception. This can result in disabling instrumentations. */
  public static void recordException(Throwable throwable) {
    if (!(throwable instanceof ClassCastException)) {
      return;
    }
    String message = throwable.getMessage();
    if (message == null || message.isEmpty() || !isHypertraceType(message)) {
      return;
    }
    message = message.toLowerCase();
    if (message.contains("servlet")) {
      log.error(
          "Hypertrace servlet type caused class cast exception. Disabling wrapping of servlet types",
          throwable);
      servletCausingException = true;
    }
  }

  private static boolean isHypertraceType(String message) {
    return message.contains("hypertrace");
  }

  /** Reset the config, use only in tests. */
  @VisibleForTesting
  public static void reset() {
    synchronized (HypertraceConfig.class) {
      agentConfig = null;
    }
  }

  @VisibleForTesting
  static AgentConfig load(DynamicConfigFetcher dynamicConfigFetcher) throws IOException {
    String configFile = EnvironmentConfig.getProperty(EnvironmentConfig.CONFIG_FILE_PROPERTY);
    if (configFile == null) {
      AgentConfig.Builder configBuilder = AgentConfig.newBuilder();
      if (dynamicConfigFetcher != null) {
        configBuilder = dynamicConfigFetcher.initializeConfig().toBuilder();
      }
      return EnvironmentConfig.applyPropertiesAndEnvVars(applyDefaults(configBuilder)).build();
    }
    return load(configFile, dynamicConfigFetcher);
  }

  @VisibleForTesting
  static AgentConfig load(String filename, DynamicConfigFetcher dynamicConfigFetcher)
      throws IOException {
    File configFile = new File(filename);
    if (!configFile.exists() || configFile.isDirectory() || !configFile.canRead()) {
      log.error("Config file {} does not exist", filename);
      AgentConfig.Builder configBuilder = AgentConfig.newBuilder();
      if (dynamicConfigFetcher != null) {
        configBuilder = dynamicConfigFetcher.initializeConfig().toBuilder();
      }
      return EnvironmentConfig.applyPropertiesAndEnvVars(applyDefaults(configBuilder)).build();
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
    if (!configBuilder.hasEnabled()) {
      configBuilder.setEnabled(BoolValue.newBuilder().setValue(true).build());
    }

    Reporting.Builder reportingBuilder =
        applyReportingDefaults(configBuilder.getReporting().toBuilder());
    configBuilder.setReporting(reportingBuilder);

    DataCapture.Builder dataCaptureBuilder =
        setDefaultsToDataCapture(configBuilder.getDataCapture().toBuilder());
    configBuilder.setDataCapture(dataCaptureBuilder);

    if (configBuilder.getPropagationFormatsList().isEmpty()) {
      configBuilder.addPropagationFormats(PropagationFormat.TRACECONTEXT);
    }
    return configBuilder;
  }

  private static Reporting.Builder applyReportingDefaults(Reporting.Builder builder) {
    if (!builder.hasEndpoint()) {
      builder.setEndpoint(StringValue.newBuilder().setValue(DEFAULT_REPORTING_ENDPOINT).build());
    }
    if (builder.getTraceReporterType().equals(Config.TraceReporterType.UNSPECIFIED)) {
      builder.setTraceReporterType(Config.TraceReporterType.ZIPKIN);
    }
    Builder opaBuilder = applyOpaDefaults(builder.getOpa().toBuilder());
    builder.setOpa(opaBuilder);
    return builder;
  }

  private static Opa.Builder applyOpaDefaults(Opa.Builder builder) {
    if (!builder.hasEndpoint()) {
      builder.setEndpoint(StringValue.newBuilder().setValue(DEFAULT_OPA_ENDPOINT).build());
    }
    if (!builder.hasPollPeriodSeconds()) {
      builder.setPollPeriodSeconds(
          Int32Value.newBuilder().setValue(DEFAULT_OPA_POLL_PERIOD_SECONDS).build());
    }
    return builder;
  }

  private static DataCapture.Builder setDefaultsToDataCapture(DataCapture.Builder builder) {
    builder.setHttpHeaders(applyMessageDefaults(builder.getHttpHeaders().toBuilder()));
    builder.setHttpBody(applyMessageDefaults(builder.getHttpBody().toBuilder()));
    builder.setRpcMetadata(applyMessageDefaults(builder.getRpcMetadata().toBuilder()));
    builder.setRpcBody(applyMessageDefaults(builder.getRpcBody().toBuilder()));
    if (!builder.hasBodyMaxSizeBytes()) {
      builder.setBodyMaxSizeBytes(
          Int32Value.newBuilder().setValue(DEFAULT_BODY_MAX_SIZE_BYTES).build());
    }
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

  public static class DynamicConfigFetcher implements Runnable {

    private final ConfigurationServiceGrpc.ConfigurationServiceBlockingStub blockingStub;

    private static Int64Value configUpdatedTimestamp;

    private DynamicConfigFetcher(String dynamicConfigServiceUrl) {
      Channel channel =
          ManagedChannelBuilder.forTarget(dynamicConfigServiceUrl).usePlaintext().build();
      blockingStub = ConfigurationServiceGrpc.newBlockingStub(channel);
      configUpdatedTimestamp = Int64Value.newBuilder().setValue(System.currentTimeMillis()).build();
    }

    @Override
    public void run() {
      Service.UpdateConfigurationResponse response =
          blockingStub.updateConfiguration(
              Service.UpdateConfigurationRequest.newBuilder()
                  .setTimestamp(configUpdatedTimestamp)
                  .build());
      configUpdatedTimestamp = response.getTimestamp();
      AgentConfig.Builder configBuilder = HypertraceConfig.get().toBuilder();
      configBuilder.setEnabled(response.getEnabled());
      configBuilder.setDataCapture(response.getDataCapture());
      configBuilder.setJavaagent(response.getJavaAgent());
      agentConfig = configBuilder.build();
    }

    private AgentConfig initializeConfig() {
      Service.InitialConfigurationResponse response =
          blockingStub.initialConfiguration(
              Service.InitialConfigurationRequest.newBuilder().build());
      configUpdatedTimestamp = response.getTimestamp();
      return response.getAgentConfig();
    }
  }
}
