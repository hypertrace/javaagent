package org.hypertrace.agent.otel.extensions;

import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import org.hypertrace.agent.core.bootstrap.config.InstrumentationConfigProvider;

public class HypertraceInstrumentationConfigComponentInstaller implements ComponentInstaller {

  @Override
  public void beforeByteBuddyAgent(Config config) {
    // trigger registration
    InstrumentationConfigProvider.get();
  }
}
