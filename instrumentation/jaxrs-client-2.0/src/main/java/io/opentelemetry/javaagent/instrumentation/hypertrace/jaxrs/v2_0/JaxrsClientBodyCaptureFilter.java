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

package io.opentelemetry.javaagent.instrumentation.hypertrace.jaxrs.v2_0;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxrsClientBodyCaptureFilter implements ClientRequestFilter, ClientResponseFilter {

  private static final Logger log = LoggerFactory.getLogger(JaxrsClientBodyCaptureFilter.class);
  private static final InstrumentationConfig instrumentationConfig =
      InstrumentationConfig.ConfigProvider.get();
  /** TODO find a better way to access this */
  public static final String OTEL_CONTEXT_PROPERTY_NAME = "io.opentelemetry.javaagent.context";
  /**
   * In certain contexts, like reading HTTP client entities, the body will be read after the OTEL
   * context has been removed from the carrier. In order to mitigate this, we re-store a reference
   * to the object under a different property key and remove it when it is no longer needed
   */
  public static final String HYPERTRACE_CONTEXT_PROPERTY_NAME = "org.hypertrace.javaagent.context";

  @Override
  public void filter(ClientRequestContext requestContext) {
    Object contextObj = requestContext.getProperty(OTEL_CONTEXT_PROPERTY_NAME);
    if (!(contextObj instanceof Context)) {
      return;
    }
    requestContext.setProperty(HYPERTRACE_CONTEXT_PROPERTY_NAME, contextObj);

    Context currentContext = (Context) contextObj;
    Span currentSpan = Span.fromContext(currentContext);

    try {
      if (instrumentationConfig.httpHeaders().request()) {
        captureHeaders(
            currentSpan,
            HypertraceSemanticAttributes::httpRequestHeader,
            requestContext.getStringHeaders());
      }
    } catch (Exception ex) {
      log.error("Exception while getting request headers", ex);
    }
  }

  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
    Object contextObj = requestContext.getProperty(OTEL_CONTEXT_PROPERTY_NAME);
    if (!(contextObj instanceof Context)) {
      return;
    }

    Context currentContext = (Context) contextObj;
    Span currentSpan = Span.fromContext(currentContext);

    try {
      if (instrumentationConfig.httpHeaders().response()) {
        captureHeaders(
            currentSpan,
            HypertraceSemanticAttributes::httpResponseHeader,
            responseContext.getHeaders());
      }
    } catch (Exception ex) {
      log.error("Exception while getting response headers", ex);
    }
  }

  private static void captureHeaders(
      Span span,
      Function<String, AttributeKey<String>> keySupplier,
      MultivaluedMap<String, String> headers) {
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      for (Object value : entry.getValue()) {
        span.setAttribute(keySupplier.apply(entry.getKey()), value.toString());
      }
    }
  }
}
