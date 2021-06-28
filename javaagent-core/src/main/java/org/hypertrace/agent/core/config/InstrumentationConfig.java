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
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Instrumentation config holds configuration for the instrumentation. */
public interface InstrumentationConfig {

  /** Maximum capture body size in bytes. */
  int maxBodySizeBytes();

  /** Data capture for HTTP headers. */
  Message httpHeaders();

  /** Data capture for HTTP body. */
  Message httpBody();

  /** Data capture for RPC metadata. */
  Message rpcMetadata();

  /** Data capture for RPC body */
  Message rpcBody();

  /** Message holds data capture configuration for various entities. */
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
        && !rpcBody().request()
        && !rpcBody().response()
        && !rpcMetadata().request()
        && !rpcMetadata().response()) {
      return false;
    }
    return true;
  }

  class ConfigProvider {
    private static final Logger logger = LoggerFactory.getLogger(ConfigProvider.class);

    private static volatile InstrumentationConfig instrumentationConfig;

    /** Reset the config, use only in tests. */
    public static void reset() {
      synchronized (ConfigProvider.class) {
        instrumentationConfig = null;
      }
    }

    private static InstrumentationConfig load() {
      ServiceLoader<InstrumentationConfig> configs =
          ServiceLoader.load(InstrumentationConfig.class);
      Iterator<InstrumentationConfig> iterator = configs.iterator();
      if (!iterator.hasNext()) {
        logger.error("Failed to load instrumentation config");
        return null;
      }
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
