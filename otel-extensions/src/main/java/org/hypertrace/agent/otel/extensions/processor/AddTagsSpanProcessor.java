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

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.hypertrace.agent.otel.extensions.CgroupsReader;

public class AddTagsSpanProcessor implements SpanProcessor {

  // initialize at startup because the processor is executed for every span.
  private String containerId;

  /** Note - the container id is not available using this technique if cgroup2 is installed. */
  public AddTagsSpanProcessor() {
    CgroupsReader cgroupsReader = new CgroupsReader();
    containerId = cgroupsReader.readContainerId();
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    if (containerId != null && !containerId.isEmpty()) {
      span.setAttribute(ResourceAttributes.CONTAINER_ID, containerId);
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return CompletableResultCode.ofSuccess();
  }
}
