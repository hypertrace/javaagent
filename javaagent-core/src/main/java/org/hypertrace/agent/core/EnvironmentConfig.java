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

import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.config.Config.DataCapture;
import org.hypertrace.agent.config.Config.Message;
import org.hypertrace.agent.config.Config.Reporting;

public class EnvironmentConfig {

  private EnvironmentConfig() {}

  private static final String HT_PREFIX = "ht.";

  public static final String CONFIG_FILE_PROPERTY = HT_PREFIX + "config.file";
  static final String SERVICE_NAME = HT_PREFIX + "service.name";

  private static final String REPORTING_PREFIX = HT_PREFIX + "reporting.";
  static final String REPORTING_ADDRESS = REPORTING_PREFIX + "address";
  static final String REPORTING_SECURE = REPORTING_PREFIX + "secure";

  private static final String CAPTURE_PREFIX = HT_PREFIX + "data.capture.";
  public static final String CAPTURE_HTTP_HEADERS_PREFIX = CAPTURE_PREFIX + "http.headers.";
  public static final String CAPTURE_HTTP_BODY_PREFIX = CAPTURE_PREFIX + "http.body.";
  public static final String CAPTURE_RPC_METADATA_PREFIX = CAPTURE_PREFIX + "rpc.metadata.";
  public static final String CAPTURE_RPC_BODY_PREFIX = CAPTURE_PREFIX + "rpc.body.";

  public static AgentConfig.Builder applyPropertiesAndEnvVars(AgentConfig.Builder builder) {
    String serviceName = getProperty(SERVICE_NAME);
    if (serviceName != null) {
      builder.setServiceName(StringValue.newBuilder().setValue(serviceName).build());
    }

    Reporting.Builder reportingBuilder = applyReporting(builder.getReporting().toBuilder());
    builder.setReporting(reportingBuilder);

    DataCapture.Builder dataCaptureBuilder =
        setDefaultsToDataCapture(builder.getDataCapture().toBuilder());
    builder.setDataCapture(dataCaptureBuilder);
    return builder;
  }

  private static Reporting.Builder applyReporting(Reporting.Builder builder) {
    String reporterAddress = getProperty(REPORTING_ADDRESS);
    if (reporterAddress != null) {
      builder.setAddress(StringValue.newBuilder().setValue(reporterAddress).build());
    }
    String secure = getProperty(REPORTING_SECURE);
    if (secure != null) {
      builder.setSecure(BoolValue.newBuilder().setValue(Boolean.valueOf(secure)).build());
    }

    return builder;
  }

  private static DataCapture.Builder setDefaultsToDataCapture(DataCapture.Builder builder) {
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
