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

import io.opentelemetry.api.trace.Span;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SpanUtils {

  static final String AGENT_VERSION_ATTRIBUTE_KEY = "otel.extension.library.version";
  private static final Logger logger = LoggerFactory.getLogger(SpanUtils.class.getName());

  private static final ConcurrentMap<String, String> ATTRIBUTE_MAP = new ConcurrentHashMap<>();

  private static Supplier<String> agentVersionSupplier = () -> getAgentVersion();

  private static boolean errorObtainingAgentVersion;

  /**
   * Obtains the current version of the Hypertrace agent. This is used to populate the
   * "otel.extension.library.version" that is added to the Span.
   *
   * @return
   */
  private static boolean setAgentVersion() {
    String agentVersion = ATTRIBUTE_MAP.get(AGENT_VERSION_ATTRIBUTE_KEY);

    if (agentVersion == null && !errorObtainingAgentVersion) {
      agentVersion = agentVersionSupplier.get();

      if (agentVersion != null) {
        ATTRIBUTE_MAP.put(AGENT_VERSION_ATTRIBUTE_KEY, agentVersion);
      } else {
        errorObtainingAgentVersion = true;
      }
    }
    return !errorObtainingAgentVersion;
  }

  /**
   * Adds the hypertrace-specific attributes to the Span.
   *
   * @param span
   */
  public static void setSpanAttributes(Span span) {
    if (span == null) {
      return;
    }

    collect();

    for (Map.Entry<String, String> nextAttribute : ATTRIBUTE_MAP.entrySet()) {
      span.setAttribute(nextAttribute.getKey(), nextAttribute.getValue());
    }
  }

  /**
   * Collects the data that is used for the attribute values.
   *
   * @return
   */
  private static boolean collect() {
    if (!setAgentVersion()) {
      return false;
    }

    return true;
  }

  /**
   * Obtain the agent version by obtaining the implementation version of the HypertraceAgent class.
   *
   * @return
   */
  static String getAgentVersion() {
    try {
      Class<?> clazz = Class.forName("org.hypertrace.agent.instrument.HypertraceAgent", true, null);
      return clazz.getPackage().getImplementationVersion();
    } catch (ClassNotFoundException e) {
      logger.warn("Could not load HypertraceAgent class");
      return null;
    }
  }

  /**
   * For unit tests, replace the default agentVersionSupplier.
   *
   * @param _agentVersionSupplier
   */
  static void setVersionSupplier(Supplier<String> _agentVersionSupplier) {
    agentVersionSupplier = _agentVersionSupplier;
  }
}
