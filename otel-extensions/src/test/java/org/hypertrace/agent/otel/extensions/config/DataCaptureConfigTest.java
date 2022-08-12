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

package org.hypertrace.agent.otel.extensions.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.ServiceLoader;
import org.hypertrace.agent.core.config.DataCaptureConfig;
import org.junit.jupiter.api.Test;

public class DataCaptureConfigTest {

  @Test
  public void instantiateViaServiceLoaderApi() {
    final ServiceLoader<DataCaptureConfig> dataCaptureConfigs =
        ServiceLoader.load(DataCaptureConfig.class);
    final Iterator<DataCaptureConfig> iterator = dataCaptureConfigs.iterator();
    assertTrue(iterator.hasNext());
    final DataCaptureConfig dataCaptureConfig = iterator.next();
    assertNotNull(dataCaptureConfig);
    assertFalse(iterator.hasNext());
  }
}
