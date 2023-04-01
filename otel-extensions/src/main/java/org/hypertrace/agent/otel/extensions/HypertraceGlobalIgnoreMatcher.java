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

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesBuilder;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesConfigurer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

@AutoService(IgnoredTypesConfigurer.class)
public class HypertraceGlobalIgnoreMatcher implements IgnoredTypesConfigurer {

  @Override
  public void configure(
      IgnoredTypesBuilder ignoredTypesBuilder, ConfigProperties configProperties) {
    ignoredTypesBuilder
        // ignored profiler classes
        .ignoreClass("com.yourkit")
        // allowed java io classes
        .allowClass("java.io.InputStream")
        .allowClass("java.io.OutputStream")
        .allowClass("java.io.ByteArrayInputStream")
        .allowClass("java.io.ByteArrayOutputStream")
        .allowClass("java.io.BufferedReader")
        .allowClass("java.io.PrintWriter")
        // ignored class loaders
        .ignoreClassLoader("com.singularity.")
        .ignoreClassLoader("com.yourkit.")
        .ignoreClassLoader("com.cisco.mtagent.");
  }
}
