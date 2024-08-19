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

package org.hypertrace.agent.otel.extensions;

import io.opentelemetry.sdk.metrics.View;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MetricViewConfiguration {

  public static View createView() {
    // Attributes to exclude
    Set<String> excludedAttributes =
        new HashSet<>(
            Arrays.asList(
                "net.sock.peer.addr",
                "net.sock.peer.port",
                "http.user_agent",
                "enduser.id",
                "http.client_ip"));

    // Build the view
    return View.builder()
        .setAttributeFilter(
            attributes -> {
              for (String attribute : excludedAttributes) {
                if (attributes.contains(attribute)) {
                  return false;
                }
              }
              return true;
            })
        .build();
  }
}
