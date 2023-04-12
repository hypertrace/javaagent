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

/**
 * Interface implemented by classes which can provide the names of the content types which can be
 * captured.
 */
public interface DataCaptureConfig {

  String[] getAllowedContentTypes();

  /** This class return an implementation of the DataCaptureConfig interface */
  class ConfigProvider {
    private static final Logger logger = LoggerFactory.getLogger(ConfigProvider.class);
    private static volatile DataCaptureConfig dataCaptureConfig;

    /**
     * Locates the first concrete implementation of the DataCaptureConfig interface.
     *
     * @return
     */
    private static DataCaptureConfig load(ClassLoader cl) {
      ServiceLoader<DataCaptureConfig> configs = ServiceLoader.load(DataCaptureConfig.class, cl);
      Iterator<DataCaptureConfig> iterator = configs.iterator();
      if (!iterator.hasNext()) {
        logger.error("Failed to load data capture config");
        return null;
      }
      return iterator.next();
    }

    /**
     * @return an implementation of the DataCaptureConfig interface, or null if one cannot be found
     */
    public static DataCaptureConfig get(ClassLoader cl) {
      if (dataCaptureConfig == null) {
        synchronized (ConfigProvider.class) {
          if (dataCaptureConfig == null) {
            dataCaptureConfig = load(cl);
          }
        }
      }
      return dataCaptureConfig;
    }

    public static DataCaptureConfig get() {
      if (dataCaptureConfig == null) {
        synchronized (ConfigProvider.class) {
          if (dataCaptureConfig == null) {
            dataCaptureConfig = load(Thread.currentThread().getContextClassLoader());
          }
        }
      }
      return dataCaptureConfig;
    }
  }
}
