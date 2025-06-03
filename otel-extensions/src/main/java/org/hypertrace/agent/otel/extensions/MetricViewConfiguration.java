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
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MetricViewConfiguration {
  private static final Logger logger = Logger.getLogger(MetricViewConfiguration.class.getName());

  // OpenTelemetry's cardinality limit property
  private static final String OTEL_CARDINALITY_LIMIT =
      "otel.experimental.metrics.cardinality.limit";

  // Default HTTP attributes to include
  private static final Set<String> KEYS_TO_RETAIN =
      new HashSet<>(
          Arrays.asList(
              "http.method",
              "http.status_code",
              "http.scheme",
              "http.route",
              "rpc.method",
              "rpc.grpc.status_code",
              "rpc.service"));

  /**
   * Creates a View with HTTP attribute filtering and cardinality limit based on OpenTelemetry
   * configuration.
   *
   * <p>The cardinality limit is set from the OpenTelemetry system property or environment variable:
   *
   * <ul>
   *   <li>System property: otel.experimental.metrics.cardinality.limit
   *   <li>Environment variable: OTEL_EXPERIMENTAL_METRICS_CARDINALITY_LIMIT
   * </ul>
   *
   * @return a configured View with HTTP attribute filtering and cardinality limit
   */
  public static View createView() {
    // Build the view with our attribute filter
    View view =
        View.builder()
            .setAttributeFilter(
                attributes -> {
                  for (String attribute : KEYS_TO_RETAIN) {
                    if (attributes.contains(attribute)) {
                      return true;
                    }
                  }
                  return false;
                })
            .build();

    Integer cardinalityLimit = getCardinalityLimit();

    /* Only apply cardinality limit if it's explicitly configured
       The global cardinality configuration field 'otel.experimental.metrics.cardinality.limit' does not apply for custom views
       Also the view builder, does not have a setter for cardinality limit in 1.33.0 SDK we use.
       So using reflection to set the cardinality limit
    */
    if (cardinalityLimit != null) {
      try {
        // Get the View class
        Class<?> viewClass = view.getClass();

        // Get the cardinalityLimit field
        Field cardinalityLimitField = viewClass.getDeclaredField("cardinalityLimit");
        cardinalityLimitField.setAccessible(true);

        // Set the cardinality limit
        cardinalityLimitField.set(view, cardinalityLimit);

      } catch (Exception e) {
        logger.log(Level.WARNING, "Failed to set cardinality limit using reflection", e);
      }
    }

    return view;
  }

  /**
   * Gets the cardinality limit from OpenTelemetry's system property or environment variable.
   *
   * @return the configured cardinality limit, or null if not configured
   */
  private static Integer getCardinalityLimit() {
    String limitValue = getProperty(OTEL_CARDINALITY_LIMIT);

    if (limitValue != null && !limitValue.isEmpty()) {
      try {
        return Integer.parseInt(limitValue);
      } catch (NumberFormatException e) {
        logger.log(Level.WARNING, "Invalid cardinality limit value: " + limitValue, e);
      }
    }

    return null; // No explicit configuration
  }

  /**
   * Gets a property from system properties or environment variables.
   *
   * @param name the property name
   * @return the property value, or null if not set
   */
  private static String getProperty(String name) {
    return System.getProperty(name, System.getenv(name.replaceAll("\\.", "_").toUpperCase()));
  }
}
