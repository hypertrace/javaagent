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

/**
 * Describes instrumentation strategies for capturing the request body on containers. Generally,
 * this is detected at runtime and stored in the {@link
 * io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext} so that {@link
 * #APP_SERVER} instrumentation can short-circuit when it is know that that the preferred {@link
 * #SERVLET} instrumentation is sufficient to capture the request body
 */
public enum RequestBodyCaptureMethod {
  /**
   * Signals that the request body should be captured relying on the instrumentation of APIs defined
   * by {@code javax.servlet} implementations
   */
  SERVLET,
  /**
   * Signals that request body should be captured relying on the instrumentation of APIs defined
   * inside the app server's implementation that do not relate to {@code javax.servlet} APIs
   */
  APP_SERVER
}
