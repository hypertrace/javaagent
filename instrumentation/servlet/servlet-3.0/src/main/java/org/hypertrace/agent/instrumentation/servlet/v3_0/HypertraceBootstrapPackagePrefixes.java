package org.hypertrace.agent.instrumentation.servlet.v3_0;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.spi.BootstrapPackages;
import java.util.Arrays;
import java.util.List;

/**
 * TODO move to a separate module e.g. `providers`
 *
 * @author Pavol Loffay
 */
@AutoService(BootstrapPackages.class)
public class HypertraceBootstrapPackagePrefixes implements BootstrapPackages {

  @Override
  public List<String> getPackagePrefixes() {
    return Arrays.asList("org.hypertrace.agent.blocking");
  }
}
