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

package org.hypertrace.agent.core.instrumentation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HypertraceSemanticAttributesTest {

  @Test
  public void caseInsensitiveHttpRequestHeader() {
    Assertions.assertEquals(
        "http.request.header.content-type",
        HypertraceSemanticAttributes.httpRequestHeader("Content-Type").getKey());
  }

  @Test
  public void caseInsensitiveHttpResponseHeader() {
    Assertions.assertEquals(
        "http.response.header.content-type",
        HypertraceSemanticAttributes.httpResponseHeader("Content-Type").getKey());
  }

  @Test
  public void caseInsensitiveRpcRequestMetadata() {
    Assertions.assertEquals(
        "rpc.request.metadata.md", HypertraceSemanticAttributes.rpcRequestMetadata("MD").getKey());
  }

  @Test
  public void caseInsensitiveRpcResponseMetadata() {
    Assertions.assertEquals(
        "rpc.response.metadata.md",
        HypertraceSemanticAttributes.rpcResponseMetadata("MD").getKey());
  }
}
