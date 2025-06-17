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

package org.hypertrace.agent.otel.extensions.config;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.hypertrace.agent.config.v1.Config.AgentConfig;
import org.hypertrace.agent.config.v1.Config.DataCapture;
import org.hypertrace.agent.config.v1.Config.Message;
import org.hypertrace.agent.config.v1.Config.MetricReporterType;
import org.hypertrace.agent.config.v1.Config.PropagationFormat;
import org.hypertrace.agent.config.v1.Config.Reporting;
import org.hypertrace.agent.config.v1.Config.TraceReporterType;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HypertraceConfig} loads a yaml config from file. */
public class HypertraceConfig {

  // ---------------------------------------------------
  //  Represents the default set of content types that can be
  //  captured, if the content types are not specified in the config.
  // ---------------------------------------------------
  private static final List<StringValue> DEFAULT_CONTENT_TYPES = initDefaultContentTypes();

  private HypertraceConfig() {}

  private static List<StringValue> initDefaultContentTypes() {
    String[] defaultContentTypes = ContentTypeUtils.getDefaultContentTypes();

    StringValue[] defaultContentTypeValues = new StringValue[defaultContentTypes.length];

    for (int i = 0; i < defaultContentTypes.length; i++) {
      defaultContentTypeValues[i] =
          StringValue.newBuilder().setValue(defaultContentTypes[i]).build();
    }

    return Arrays.asList(defaultContentTypeValues);
  }

  private static final Logger log = LoggerFactory.getLogger(HypertraceConfig.class);

  // volatile field in order to properly handle lazy initialization with double-checked locking
  private static volatile AgentConfig agentConfig;

  static final String DEFAULT_SERVICE_NAME = "unknown";
  // Default reporting endpoint for traces and metrics
  static final String DEFAULT_REPORTING_ENDPOINT = "http://localhost:5442";
  // 128 KiB
  static final int DEFAULT_BODY_MAX_SIZE_BYTES = 128 * 1024;

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

  /** Reset the config, use only in tests. */
  @VisibleForTesting
  public static void reset() {
    synchronized (HypertraceConfig.class) {
      agentConfig = null;
    }
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
      AgentConfig.Builder configBuilder = AgentConfig.newBuilder();
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
    if (builder.getTraceReporterType().equals(TraceReporterType.UNSPECIFIED)) {
      builder.setTraceReporterType(TraceReporterType.OTLP);
    }
    if (!builder.hasMetricEndpoint()) {
      if (TraceReporterType.OTLP.equals(builder.getTraceReporterType())) {
        // If trace reporter type is OTLP, use the same endpoint for metrics
        builder.setMetricEndpoint(builder.getEndpoint());
      } else {
        builder.setMetricEndpoint(
            StringValue.newBuilder().setValue(DEFAULT_REPORTING_ENDPOINT).build());
      }
    }
    if (builder
        .getMetricReporterType()
        .equals(MetricReporterType.METRIC_REPORTER_TYPE_UNSPECIFIED)) {
      builder.setMetricReporterType(MetricReporterType.METRIC_REPORTER_TYPE_OTLP);
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

    Collection<StringValue> contentTypeList =
        applyListDefaults(builder.getAllowedContentTypesList(), () -> DEFAULT_CONTENT_TYPES);
    builder.clearAllowedContentTypes().addAllAllowedContentTypes(contentTypeList);
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

  /**
   * Creates a collection of objects consisting of the set of objects specified in the
   * configuration. If there are no objects in the configuration, the default set of objects is
   * used.
   *
   * @param originalList the set of objects specified in the configuration
   * @param defaultSupplier a lambda which provides the default set of objects.
   * @return a collection of objects, consisting of the union of originalList, with the list
   *     provided by defaultSupplier
   * @param <T>
   */
  private static <T> Collection<T> applyListDefaults(
      List<T> originalList, Supplier<List<T>> defaultSupplier) {
    Set<T> returnSet = new HashSet<>();

    if (originalList != null && originalList.size() > 0) {
      returnSet.addAll(originalList);
    } else {
      returnSet.addAll(defaultSupplier.get());
    }

    return returnSet;
  }
}
