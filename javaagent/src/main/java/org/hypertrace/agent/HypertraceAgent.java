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

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.lang.instrument.Instrumentation;

public class HypertraceAgent {
  // https://github.com/open-telemetry/opentelemetry-specification/blob/master/specification/sdk-environment-variables.md
  private static final String OTEL_EXPORTER = "otel.exporter";
  private static final String OTEL_EXPORTER_ZIPKIN_ENDPOINT = "otel.exporter.zipkin.endpoint";
  private static final String OTEL_EXPORTER_ZIPKIN_SERVICE_NAME =
      "otel.exporter.zipkin.service.name";
  private static final String OTEL_PROCESSOR_BATCH_MAX_QUEUE = "otel.bsp.max.queue.size";
  private static final String OTEL_DEFAULT_LOG_LEVEL =
      "io.opentelemetry.javaagent.slf4j.simpleLogger.defaultLogLevel";

  public static void premain(String agentArgs, Instrumentation inst) {
    agentmain(agentArgs, inst);
  }

  public static void agentmain(String agentArgs, Instrumentation inst) {
    OpenTelemetryAgent.premain(agentArgs, inst);
  }
}
