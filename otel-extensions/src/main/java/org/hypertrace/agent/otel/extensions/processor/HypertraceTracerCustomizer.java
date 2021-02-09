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

package org.hypertrace.agent.otel.extensions.processor;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.spi.TracerCustomizer;
import io.opentelemetry.sdk.trace.SdkTracerManagement;

/**
 * This is a workaround to add container ID tags to spans when Zipkin exporter is used. Zipkin
 * exporter does not add process attributes where the container ID is
 * https://github.com/open-telemetry/opentelemetry-java/issues/1970.
 *
 * <p>Remove this once we migrate to OTEL exporter
 * https://github.com/hypertrace/javaagent/issues/132
 */
@AutoService(TracerCustomizer.class)
public class HypertraceTracerCustomizer implements TracerCustomizer {

  @Override
  public void configure(SdkTracerManagement tracerManagement) {
    tracerManagement.addSpanProcessor(new AddTagsSpanProcessor());
  }
}
