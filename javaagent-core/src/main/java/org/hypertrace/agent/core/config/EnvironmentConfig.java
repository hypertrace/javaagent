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

import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.config.Config.DataCapture;
import org.hypertrace.agent.config.Config.JavaAgent;
import org.hypertrace.agent.config.Config.Message;
import org.hypertrace.agent.config.Config.Opa;
import org.hypertrace.agent.config.Config.Opa.Builder;
import org.hypertrace.agent.config.Config.PropagationFormat;
import org.hypertrace.agent.config.Config.Reporting;
import org.hypertrace.agent.config.Config.TraceReporterType;

public class EnvironmentConfig {

  private EnvironmentConfig() {}

  private static final String HT_PREFIX = "ht.";

  public static final String CONFIG_FILE_PROPERTY = HT_PREFIX + "config.file";
  public static final String DYNAMIC_CONFIG_SERVICE_URL = HT_PREFIX + "dynamic.config.service.url";
  static final String SERVICE_NAME = HT_PREFIX + "service.name";
  static final String ENABLED = HT_PREFIX + "enabled";
  static final String RESOURCE_ATTRIBUTES = HT_PREFIX + ".resource.attributes";

  static final String PROPAGATION_FORMATS = HT_PREFIX + "propagation.formats";

  private static final String REPORTING_PREFIX = HT_PREFIX + "reporting.";
  static final String REPORTING_ENDPOINT = REPORTING_PREFIX + "endpoint";
  static final String REPORTING_TRACE_TYPE = REPORTING_PREFIX + "trace.reporter.type";
  static final String REPORTING_SECURE = REPORTING_PREFIX + "secure";

  private static final String OPA_PREFIX = REPORTING_PREFIX + "opa.";
  static final String OPA_ENDPOINT = OPA_PREFIX + "endpoint";
  static final String OPA_POLL_PERIOD = OPA_PREFIX + "poll.period.seconds";
  static final String OPA_ENABLED = OPA_PREFIX + "enabled";

  private static final String CAPTURE_PREFIX = HT_PREFIX + "data.capture.";
  public static final String CAPTURE_BODY_MAX_SIZE_BYTES = CAPTURE_PREFIX + "body.max.size.bytes";
  public static final String CAPTURE_HTTP_HEADERS_PREFIX = CAPTURE_PREFIX + "http.headers.";
  public static final String CAPTURE_HTTP_BODY_PREFIX = CAPTURE_PREFIX + "http.body.";
  public static final String CAPTURE_RPC_METADATA_PREFIX = CAPTURE_PREFIX + "rpc.metadata.";
  public static final String CAPTURE_RPC_BODY_PREFIX = CAPTURE_PREFIX + "rpc.body.";

  private static final String JAVAAGENT_PREFIX = HT_PREFIX + "javaagent.";
  public static final String JAVAAGENT_FILTER_JAR_PATHS = JAVAAGENT_PREFIX + "filter.jar.paths";

  public static AgentConfig.Builder applyPropertiesAndEnvVars(AgentConfig.Builder builder) {
    String serviceName = getProperty(SERVICE_NAME);
    if (serviceName != null) {
      builder.setServiceName(StringValue.newBuilder().setValue(serviceName).build());
    }
    String enabled = getProperty(ENABLED);
    if (enabled != null) {
      builder.setEnabled(BoolValue.newBuilder().setValue(Boolean.valueOf(enabled)).build());
    }

    String attributes = getProperty(RESOURCE_ATTRIBUTES);
    if (attributes != null) {
      String[] attrs = attributes.split(",");
      for (String attr : attrs) {
        String[] keyValArr = attr.split("=");
        if (keyValArr.length == 2) {
          String key = keyValArr[0];
          String val = keyValArr[1];
          builder.putResourceAttributes(key, val);
        }
      }
    }

    Reporting.Builder reportingBuilder = applyReporting(builder.getReporting().toBuilder());
    builder.setReporting(reportingBuilder);

    DataCapture.Builder dataCaptureBuilder =
        setDefaultsToDataCapture(builder.getDataCapture().toBuilder());
    builder.setDataCapture(dataCaptureBuilder);
    applyPropagationFormat(builder);
    JavaAgent.Builder javaagentBuilder = applyJavaAgent(builder.getJavaagentBuilder());
    builder.setJavaagent(javaagentBuilder);
    return builder;
  }

  private static JavaAgent.Builder applyJavaAgent(JavaAgent.Builder builder) {
    String filterJarPaths = getProperty(JAVAAGENT_FILTER_JAR_PATHS);
    if (filterJarPaths != null) {
      builder.clearFilterJarPaths();
      String[] jarPaths = filterJarPaths.split(",");
      for (String jarPath : jarPaths) {
        builder.addFilterJarPaths(StringValue.newBuilder().setValue(jarPath));
      }
    }
    return builder;
  }

  private static void applyPropagationFormat(AgentConfig.Builder builder) {
    String propagationFormats = getProperty(PROPAGATION_FORMATS);
    if (propagationFormats != null) {
      builder.clearPropagationFormats();
      String[] formats = propagationFormats.split(",");
      for (String format : formats) {
        builder.addPropagationFormats(PropagationFormat.valueOf(format));
      }
    }
  }

  private static Reporting.Builder applyReporting(Reporting.Builder builder) {
    String reporterAddress = getProperty(REPORTING_ENDPOINT);
    if (reporterAddress != null) {
      builder.setEndpoint(StringValue.newBuilder().setValue(reporterAddress).build());
    }
    String traceReportingType = getProperty(REPORTING_TRACE_TYPE);
    if (traceReportingType != null) {
      builder.setTraceReporterType(TraceReporterType.valueOf(traceReportingType));
    }
    String secure = getProperty(REPORTING_SECURE);
    if (secure != null) {
      builder.setSecure(BoolValue.newBuilder().setValue(Boolean.valueOf(secure)).build());
    }
    Builder opaBuilder = applyOpa(builder.getOpa().toBuilder());
    builder.setOpa(opaBuilder);
    return builder;
  }

  private static Opa.Builder applyOpa(Opa.Builder builder) {
    String address = getProperty(OPA_ENDPOINT);
    if (address != null) {
      builder.setEndpoint(StringValue.newBuilder().setValue(address).build());
    }
    String pollPeriod = getProperty(OPA_POLL_PERIOD);
    if (pollPeriod != null) {
      builder.setPollPeriodSeconds(
          Int32Value.newBuilder().setValue(Integer.parseInt(pollPeriod)).build());
    }
    String enabled = getProperty(OPA_ENABLED);
    if (enabled != null) {
      builder.setEnabled(BoolValue.newBuilder().setValue(Boolean.valueOf(enabled)).build());
    }
    return builder;
  }

  private static DataCapture.Builder setDefaultsToDataCapture(DataCapture.Builder builder) {
    String bodyMaxSizeBytes = getProperty(CAPTURE_BODY_MAX_SIZE_BYTES);
    if (bodyMaxSizeBytes != null) {
      builder.setBodyMaxSizeBytes(
          Int32Value.newBuilder().setValue(Integer.valueOf(bodyMaxSizeBytes)).build());
    }
    builder.setHttpHeaders(
        applyMessageDefaults(builder.getHttpHeaders().toBuilder(), CAPTURE_HTTP_HEADERS_PREFIX));
    builder.setHttpBody(
        applyMessageDefaults(builder.getHttpBody().toBuilder(), CAPTURE_HTTP_BODY_PREFIX));
    builder.setRpcMetadata(
        applyMessageDefaults(builder.getRpcMetadata().toBuilder(), CAPTURE_RPC_METADATA_PREFIX));
    builder.setRpcBody(
        applyMessageDefaults(builder.getRpcBody().toBuilder(), CAPTURE_RPC_BODY_PREFIX));
    return builder;
  }

  private static Message.Builder applyMessageDefaults(Message.Builder builder, String prefix) {
    String request = getProperty(prefix + "request");
    if (request != null) {
      builder.setRequest(BoolValue.newBuilder().setValue(Boolean.valueOf(request)).build());
    }
    String response = getProperty(prefix + "response");
    if (request != null) {
      builder.setResponse(BoolValue.newBuilder().setValue(Boolean.valueOf(response)).build());
    }
    return builder;
  }

  public static String getProperty(String name) {
    return System.getProperty(name, System.getenv(name.replaceAll("\\.", "_").toUpperCase()));
  }
}
