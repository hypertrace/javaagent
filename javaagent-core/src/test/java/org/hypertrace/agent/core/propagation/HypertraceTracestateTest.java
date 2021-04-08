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
import org.hypertrace.agent.core.propagation.HypertraceTracestate.CaptureMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HypertraceTracestateTest {

  @Test
  public void create() {
    Assertions.assertEquals(
        "cap:1",
        HypertraceTracestate.create(TraceState.getDefault(), CaptureMode.ALL)
            .get(HypertraceTracestate.KEY));
    Assertions.assertEquals(
        "cap:0",
        HypertraceTracestate.create(TraceState.getDefault(), CaptureMode.DEFAULT)
            .get(HypertraceTracestate.KEY));
  }

  @Test
  public void parseProtectionMode_advanced() {
    TraceState traceState =
        TraceState.getDefault().toBuilder().put(HypertraceTracestate.KEY, "cap:1").build();
    CaptureMode captureMode = HypertraceTracestate.getProtectionMode(traceState);
    Assertions.assertEquals(CaptureMode.ALL, captureMode);
  }

  @Test
  public void parseProtectionMode_core() {
    TraceState traceState =
        TraceState.getDefault().toBuilder().put(HypertraceTracestate.KEY, "cap:0").build();
    CaptureMode captureMode = HypertraceTracestate.getProtectionMode(traceState);
    Assertions.assertEquals(CaptureMode.DEFAULT, captureMode);
  }

  @Test
  public void parseProtectionMode_unknown() {
    TraceState traceState =
        TraceState.getDefault().toBuilder().put(HypertraceTracestate.KEY, "cap:2").build();
    CaptureMode captureMode = HypertraceTracestate.getProtectionMode(traceState);
    Assertions.assertEquals(CaptureMode.UNDEFINED, captureMode);

    traceState = TraceState.getDefault().toBuilder().put(HypertraceTracestate.KEY, "cap:2").build();
    captureMode = HypertraceTracestate.getProtectionMode(traceState);
    Assertions.assertEquals(CaptureMode.UNDEFINED, captureMode);

    traceState = TraceState.getDefault().toBuilder().put(HypertraceTracestate.KEY, " ").build();
    captureMode = HypertraceTracestate.getProtectionMode(traceState);
    Assertions.assertEquals(CaptureMode.UNDEFINED, captureMode);
  }
}
