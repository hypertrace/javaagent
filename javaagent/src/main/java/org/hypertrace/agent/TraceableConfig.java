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

package org.hypertrace.agent;

import ai.traceable.agent.agentconfig.InstrumentationAgentConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class TraceableConfig {
  private static final String CONFIG_PREFIX = "traceable";
  private static final String CONFIG_FILE_ARG = CONFIG_PREFIX + "ConfigFile";
  private static final String SERVICE_NAME_ARG = CONFIG_PREFIX + "ServiceName";

  static InstrumentationAgentConfig loadConfigFile(String agentArgs) {
    Map<String, String> agentArgsMap = parseAgentArgs(agentArgs);
    String configFile = agentArgsMap.get(CONFIG_FILE_ARG);
    if (configFile == null || configFile.isEmpty()) {
      System.err.println("Traceable.ai config file not specified");
      return null;
    }

    InstrumentationAgentConfig config;
    try (FileReader reader = new FileReader(configFile)) {
      ObjectMapper objectMapper = new ObjectMapper();
      config = objectMapper.readValue(reader, InstrumentationAgentConfig.class);
    } catch (Exception e) {
      System.err.printf("Problem reading Traceable.ai config file: %s\n", e);
      return null;
    }
    // allow overriding of the service name from the args
    String serviceName = agentArgsMap.get(SERVICE_NAME_ARG);
    if (serviceName != null && !serviceName.isEmpty()) {
      config.setServiceName(serviceName);
    }
    return config;
  }

  // Expected format is "arg1=val1,arg2=val2,arg3=val3"
  private static Map<String, String> parseAgentArgs(String agentArgs) {
    if (agentArgs == null || agentArgs.isEmpty()) {
      return Collections.emptyMap();
    }

    String[] agentArgsArr = agentArgs.split(",");
    Map<String, String> agentArgsMap = new LinkedHashMap<>(agentArgsArr.length);
    for (String arg : agentArgsArr) {
      String[] splitAgentArg = arg.split("=");
      if (splitAgentArg.length != 2) {
        throw new IllegalArgumentException(
            "Agent args is not well formed. Problem with: "
                + arg
                + ".Please use the format \"arg1=val1,arg2=val2,arg3=val3\"");
      }
      agentArgsMap.put(splitAgentArg[0], splitAgentArg[1]);
    }
    return agentArgsMap;
  }
}
