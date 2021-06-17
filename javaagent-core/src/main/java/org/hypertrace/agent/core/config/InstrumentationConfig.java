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

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/** Instrumentation config holds configuration for the instrumentation. */
public interface InstrumentationConfig {

  int maxBodySizeBytes();

  List<String> jarPaths();

  Message httpHeaders();

  Message httpBody();

  Message rpcMetadata();

  Message rpcBody();

  interface Message {
    boolean request();

    boolean response();
  }

  default boolean isInstrumentationEnabled(String primaryName, String[] otherNames) {
    // the instNames is not used because the config does not support it at the moment.

    // disabled if all is disabled
    if (!httpBody().request()
        && !httpBody().response()
        && !httpHeaders().request()
        && !httpHeaders().response()
        && !rpcMetadata().request()
        && !rpcMetadata().response()) {
      return false;
    }
    return true;
  }

  class ConfigProvider {
    private static volatile InstrumentationConfig instrumentationConfig;

    static InstrumentationConfig load() {
      ServiceLoader<InstrumentationConfig> configs =
          ServiceLoader.load(InstrumentationConfig.class);
      Iterator<InstrumentationConfig> iterator = configs.iterator();
      if (!iterator.hasNext()) {
        return null;
      }
      // TODO consider returning noop
      return iterator.next();
    }

    public static InstrumentationConfig get() {
      if (instrumentationConfig == null) {
        synchronized (ConfigProvider.class) {
          if (instrumentationConfig == null) {
            instrumentationConfig = load();
          }
        }
      }
      return instrumentationConfig;
    }
  }
}
