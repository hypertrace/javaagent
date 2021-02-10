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

package org.hypertrace.agent.instrument;

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HypertraceAgent {

  private static HypertraceAgent instance;

  public static void premain(String agentArgs, Instrumentation inst) {
    agentmain(agentArgs, inst);
  }

  public static void agentmain(String agentArgs, Instrumentation inst) {
    if (isTraceableAgentLoaded()) {
      System.out.println("Traceable agent is already running. Will not attempt to attach");
      return;
    }

    Map<String, String> parsedArgs = parseAgentArgs(agentArgs);
    for (Map.Entry<String, String> argEntry : parsedArgs.entrySet()) {
      System.setProperty(argEntry.getKey(), argEntry.getValue());
    }

    // TODO consider removing
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/1898
    if (instance != null) {
      System.out.println("Agent is already running. Will not attempt to attach");
      return;
    }

    instance = new HypertraceAgent();
    OpenTelemetryAgent.premain(agentArgs, inst);
    System.out.printf(
        "Hypertrace agent started, version: %s\n",
        HypertraceAgent.class.getPackage().getImplementationVersion());
  }

  // Expected format is "arg1=val1,arg2=val2,arg3=val3"
  private static Map<String, String> parseAgentArgs(String agentArgs) {
    if (agentArgs == null) {
      return Collections.emptyMap();
    }
    String[] agentArgsArr = agentArgs.split(",");
    Map<String, String> argsMap = new HashMap<>(agentArgsArr.length);
    for (String arg : agentArgsArr) {
      String[] splitAgentArg = arg.split("=");
      if (splitAgentArg.length != 2) {
        throw new IllegalArgumentException(
            String.format(
                "Agent args is not well formed: %s. Use the format \"arg1=val1,arg2=val2,arg3=val3\"",
                arg));
      }
      argsMap.put(splitAgentArg[0], splitAgentArg[1]);
    }
    return argsMap;
  }

  private static boolean isTraceableAgentLoaded() {
    try {
      Class.forName("ai.traceable.agent.bootstrap.AgentContext", true, null);
      return true;
    } catch (Throwable e) {
      return false;
    }
  }
}
