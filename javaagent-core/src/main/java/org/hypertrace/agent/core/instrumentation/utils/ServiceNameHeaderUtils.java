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

package org.hypertrace.agent.core.instrumentation.utils;

import org.hypertrace.agent.core.config.InstrumentationConfig;

/** Utility class for adding service name header to outgoing requests (exit calls). */
public class ServiceNameHeaderUtils {

  private static final String SERVICE_NAME_HEADER = "ta-client-servicename";

  private static final String serviceName = InstrumentationConfig.ConfigProvider.get().getServiceName();

  private ServiceNameHeaderUtils() {}

  public static String getClientServiceKey() {
    return SERVICE_NAME_HEADER;
  }

  /**
   * Gets the service name from HypertraceConfig.
   *
   * @return the service name configured in the agent
   */
  public static String getClientServiceName() {
    return serviceName;
  }
}
