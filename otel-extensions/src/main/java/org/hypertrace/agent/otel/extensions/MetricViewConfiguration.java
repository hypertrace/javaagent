package org.hypertrace.agent.otel.extensions;

import io.opentelemetry.sdk.metrics.View;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MetricViewConfiguration {

  public static View createHttpServerDurationView() {
    // Attributes to exclude
    Set<String> excludedAttributes =
        new HashSet<>(
            Arrays.asList(
                "net.sock.peer.addr",
                "net.sock.peer.port",
                "http.user_agent",
                "enduser.id",
                "http.client_ip"));

    // Build the view
    return View.builder()
        .setAttributeFilter(
            attributes -> {
              for (String attribute : excludedAttributes) {
                if (attributes.contains(attribute)) {
                  return false;
                }
              }
              return true;
            })
        .build();
  }
}
