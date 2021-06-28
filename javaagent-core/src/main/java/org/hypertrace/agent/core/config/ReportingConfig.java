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

package org.hypertrace.agent.core.config;

public interface ReportingConfig {

  /**
   * @return the {@link Opa} config implementation
   * @see Opa for more information on why this API is deprecated
   */
  @Deprecated
  Opa opa();

  boolean secure();

  String token();

  /**
   * Opa holds the configuration for the agent and filter implementations should interact with a
   * remote Open Policy Agent endpoint.
   *
   * <p>Note, this API is deprecated because it is a goal of the Hypertrace community to migrate
   * away form supplying this vendor-specific config and instead have authors of Hypertrace
   * extensions/filters to have an indiomatic way to extned the Hypertrace configuration properties
   * for their use cases
   */
  @Deprecated
  interface Opa {

    boolean enabled();

    String endpoint();

    int pollPeriodSeconds();
  }
}
