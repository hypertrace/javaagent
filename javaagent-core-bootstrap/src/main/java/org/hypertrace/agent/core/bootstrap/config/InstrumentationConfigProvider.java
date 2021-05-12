package org.hypertrace.agent.core.bootstrap.config;

import java.util.ServiceLoader;

public class InstrumentationConfigProvider {

  volatile private static InstrumentationConfig instrumentationConfig;

  public static InstrumentationConfig get() {
    if (instrumentationConfig == null) {
      synchronized (InstrumentationConfigProvider.class) {
        if (instrumentationConfig == null) {
            instrumentationConfig = resolve();
        }
      }
    }
    return instrumentationConfig;
  }

  private static InstrumentationConfig resolve() {
    ServiceLoader<InstrumentationConfig> configs = ServiceLoader.load(InstrumentationConfig.class);
    return configs.iterator().next();
  }
}
