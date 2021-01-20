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
import io.opentelemetry.javaagent.spi.IgnoreMatcherProvider;

@AutoService(IgnoreMatcherProvider.class)
public class HypertraceGlobalIgnoreMatcher implements IgnoreMatcherProvider {

  @Override
  public Result type(net.bytebuddy.description.type.TypeDescription target) {
    String actualName = target.getActualName();
    if (actualName.startsWith("java.io")) {
      if (actualName.equals("java.io.InputStream")
          || actualName.equals("java.io.OutputStream")
          || actualName.equals("java.io.ByteArrayInputStream")
          || actualName.equals("java.io.ByteArrayOutputStream")) {
        return Result.ALLOW;
      }
    }
    return Result.DEFAULT;
  }

  @Override
  public Result classloader(ClassLoader classLoader) {
    String name = classLoader.getClass().getName();
    if (name.startsWith("com.singularity.")) {
      return Result.IGNORE;
    }
    return Result.DEFAULT;
  }
}
