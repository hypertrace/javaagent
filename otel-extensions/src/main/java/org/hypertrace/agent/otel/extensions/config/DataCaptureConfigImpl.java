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

import com.google.auto.service.AutoService;
import com.google.protobuf.StringValue;
import java.util.List;
import org.hypertrace.agent.config.v1.Config;
import org.hypertrace.agent.core.config.DataCaptureConfig;

/**
 * An implementation of the DataCaptureConfig interface, which can provide the names of the content
 * types that can be captured.
 */
@AutoService(DataCaptureConfig.class)
public class DataCaptureConfigImpl implements DataCaptureConfig {

  private final Config.AgentConfig agentConfig = HypertraceConfig.get();

  private final Config.DataCapture dataCaptureConfig =
      (agentConfig != null) ? agentConfig.getDataCapture() : null;

  private String[] allAllowedContentTypes;

  public DataCaptureConfigImpl() {
    super();
  }

  public String[] getAllowedContentTypes() {
    if (allAllowedContentTypes != null) {
      return allAllowedContentTypes;
    }

    return loadContentTypes();
  }

  /**
   * Determines the content types that should be captured.
   *
   * @return an array of the content types
   */
  private synchronized String[] loadContentTypes() {
    if (allAllowedContentTypes == null) {
      if (dataCaptureConfig == null) {
        allAllowedContentTypes = new String[0];
      } else {
        List<StringValue> listOfTypes = dataCaptureConfig.getAllowedContentTypesList();
        String[] allAllowedContentTypes = new String[listOfTypes.size()];
        int idx = 0;
        for (StringValue nextStringValue : listOfTypes) {
          allAllowedContentTypes[idx] = nextStringValue.getValue();
          idx++;
        }

        this.allAllowedContentTypes = allAllowedContentTypes;
      }
    }

    return allAllowedContentTypes;
  }
}
