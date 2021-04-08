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

package org.hypertrace.agent.core.propagation;

import io.opentelemetry.api.trace.TraceState;

public class HypertraceTracestate {

  private HypertraceTracestate() {}

  static final String KEY = "hypertrace";

  /**
   * Create creates Hypertrace tracestate header
   *
   * @return Hypertrace tracestate header
   */
  public static TraceState create(TraceState traceState, CaptureMode captureMode) {
    return traceState.toBuilder().put(KEY, String.format("cap:%d", captureMode.value)).build();
  }

  public static CaptureMode getProtectionMode(TraceState traceState) {
    String htTraceState = traceState.get(KEY);
    if (htTraceState == null || htTraceState.isEmpty()) {
      return CaptureMode.UNDEFINED;
    }
    if (htTraceState.startsWith("cap:")) {
      try {
        String pMode = htTraceState.substring(htTraceState.indexOf(":") + 1);
        int i = Integer.parseInt(pMode);
        if (i == CaptureMode.ALL.value) {
          return CaptureMode.ALL;
        } else if (i == CaptureMode.DEFAULT.value) {
          return CaptureMode.DEFAULT;
        } else {
          return CaptureMode.UNDEFINED;
        }
      } catch (Throwable ex) {
      }
    } else {
      return CaptureMode.UNDEFINED;
    }
    return CaptureMode.ALL;
  }

  public enum CaptureMode {
    UNDEFINED(-1),
    /**
     * Headers and payloads are not collected. Except allowed headers (content-type and user-agent).
     */
    DEFAULT(0),
    /** All data is collected - payloads and headers. */
    ALL(1);

    private final int value;

    CaptureMode(int value) {
      this.value = value;
    }
  }
}
