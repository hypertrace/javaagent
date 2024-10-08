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

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.View;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hypertrace.agent.config.v1.Config.AgentConfig;
import org.hypertrace.agent.config.v1.Config.MetricReporterType;
import org.hypertrace.agent.config.v1.Config.PropagationFormat;
import org.hypertrace.agent.config.v1.Config.TraceReporterType;
import org.hypertrace.agent.otel.extensions.MetricViewConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class HypertraceAgentConfiguration implements AutoConfigurationCustomizerProvider {

  private static final Logger log = LoggerFactory.getLogger(HypertraceAgentConfiguration.class);

  // https://github.com/open-telemetry/opentelemetry-specification/blob/master/specification/sdk-environment-variables.md
  private static final String OTEL_TRACE_EXPORTER = "otel.traces.exporter";
  private static final String OTEL_METRICS_EXPORTER = "otel.metrics.exporter";
  private static final String OTEL_PROPAGATORS = "otel.propagators";
  private static final String OTEL_EXPORTER_ZIPKIN_ENDPOINT = "otel.exporter.zipkin.endpoint";
  private static final String OTEL_EXPORTER_ZIPKIN_SERVICE_NAME =
      "otel.exporter.zipkin.service.name";
  private static final String OTEL_PROCESSOR_BATCH_MAX_QUEUE = "otel.bsp.max.queue.size";
  private static final String OTEL_DEFAULT_LOG_LEVEL =
      "io.opentelemetry.javaagent.slf4j.simpleLogger.defaultLogLevel";
  private static final String OTEL_EXPORTER_OTLP_TRACES_ENDPOINT =
      "otel.exporter.otlp.traces.endpoint";
  private static final String OTEL_EXPORTER_OTLP_METRICS_ENDPOINT =
      "otel.exporter.otlp.metrics.endpoint";

  private static final String OTEL_ENABLED = "otel.javaagent.enabled";
  private static final String OTEL_OTLP_CERT = "otel.exporter.otlp.certificate";

  public Map<String, String> getProperties() {
    AgentConfig agentConfig = HypertraceConfig.get();

    Map<String, String> configProperties = new HashMap<>();
    configProperties.put(OTEL_ENABLED, String.valueOf(agentConfig.getEnabled().getValue()));
    configProperties.put(
        OTEL_TRACE_EXPORTER,
        agentConfig.getReporting().getTraceReporterType().name().toLowerCase());
    configProperties.put(
        OTEL_EXPORTER_ZIPKIN_SERVICE_NAME, agentConfig.getServiceName().getValue());
    if (agentConfig.getReporting().getTraceReporterType() == TraceReporterType.ZIPKIN) {
      configProperties.put(
          OTEL_EXPORTER_ZIPKIN_ENDPOINT, agentConfig.getReporting().getEndpoint().getValue());
    } else if (agentConfig.getReporting().getTraceReporterType() == TraceReporterType.OTLP) {
      configProperties.put(
          OTEL_EXPORTER_OTLP_TRACES_ENDPOINT, agentConfig.getReporting().getEndpoint().getValue());
    }
    if (agentConfig.getReporting().getMetricReporterType()
        == MetricReporterType.METRIC_REPORTER_TYPE_OTLP) {
      configProperties.put(
          OTEL_EXPORTER_OTLP_METRICS_ENDPOINT,
          agentConfig.getReporting().getMetricEndpoint().getValue());
    }
    if (agentConfig.getReporting().getMetricReporterType()
        == MetricReporterType.METRIC_REPORTER_TYPE_OTLP) {
      configProperties.put(OTEL_METRICS_EXPORTER, "otlp");
    } else if (agentConfig.getReporting().getMetricReporterType()
        == MetricReporterType.METRIC_REPORTER_TYPE_PROMETHEUS) {
      configProperties.put(OTEL_METRICS_EXPORTER, "prometheus");
    } else if (agentConfig.getReporting().getMetricReporterType()
        == MetricReporterType.METRIC_REPORTER_TYPE_LOGGING) {
      configProperties.put(OTEL_METRICS_EXPORTER, "logging");
    } else {
      configProperties.put(OTEL_METRICS_EXPORTER, "none");
    }
    configProperties.put(
        OTEL_PROPAGATORS, toOtelPropagators(agentConfig.getPropagationFormatsList()));
    if (agentConfig.getReporting().hasCertFile()) {
      if (agentConfig.getReporting().getTraceReporterType() == TraceReporterType.OTLP) {
        configProperties.put(OTEL_OTLP_CERT, agentConfig.getReporting().getCertFile().getValue());
      } else {
        log.warn(
            "A certificate file was configured, but a trace exporter other than OTLP was configured. If you would like to use a custom certificate file, you must use the OTLP exporter");
      }
    }

    return configProperties;
  }

  @VisibleForTesting
  public static String toOtelPropagators(List<PropagationFormat> propagationFormats) {
    return propagationFormats.stream()
        .map(v -> v.name().toLowerCase())
        .collect(Collectors.joining(","));
  }

  @Override
  public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
    autoConfigurationCustomizer.addPropertiesSupplier(this::getProperties);
    autoConfigurationCustomizer.addMeterProviderCustomizer(
        (builder, config) -> {
          View httpServerDurationView = MetricViewConfiguration.createView();
          builder.registerView(
              InstrumentSelector.builder().setName("*").build(), httpServerDurationView);
          return builder;
        });
  }
}
