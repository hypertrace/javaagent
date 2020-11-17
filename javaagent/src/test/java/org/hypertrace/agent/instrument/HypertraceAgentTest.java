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

package org.hypertrace.agent.instrument;

import java.util.Arrays;
import java.util.List;
import org.hypertrace.agent.config.Config.PropagationFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HypertraceAgentTest {

  @Test
  public void propagationFormatList() {
    List<PropagationFormat> formats =
        Arrays.asList(PropagationFormat.B3, PropagationFormat.TRACE_CONTEXT);
    Assertions.assertEquals("b3,tracecontext", HypertraceAgent.toOtelPropagators(formats));
  }
}
