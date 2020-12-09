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

package io.opentelemetry.instrumentation.hypertrace.jaxrs.v2_0;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.jaxrsclient.v2_0.ClientTracingFilter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.ContentTypeUtils;
import org.hypertrace.agent.core.HypertraceConfig;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxrsClientBodyCaptureFilter implements ClientRequestFilter, ClientResponseFilter {

  private static final Logger log = LoggerFactory.getLogger(JaxrsClientBodyCaptureFilter.class);

  @Override
  public void filter(ClientRequestContext requestContext) {
    Object spanObj = requestContext.getProperty(ClientTracingFilter.SPAN_PROPERTY_NAME);
    if (!(spanObj instanceof Span)) {
      return;
    }

    Span currentSpan = (Span) spanObj;
    AgentConfig agentConfig = HypertraceConfig.get();

    try {
      if (agentConfig.getDataCapture().getHttpHeaders().getRequest().getValue()) {
        captureHeaders(
            currentSpan,
            HypertraceSemanticAttributes::httpRequestHeader,
            requestContext.getStringHeaders());
      }
      if (requestContext.hasEntity()
          && agentConfig.getDataCapture().getHttpBody().getRequest().getValue()) {
        MediaType mediaType = requestContext.getMediaType();
        if (mediaType == null || !ContentTypeUtils.shouldCapture(mediaType.toString())) {
          return;
        }

        Object entity = requestContext.getEntity();
        if (entity != null) {
          if (entity instanceof Form) {
            Form form = (Form) entity;
            MultivaluedMap<String, String> formMap = form.asMap();
            if (formMap != null) {
              currentSpan.setAttribute(
                  HypertraceSemanticAttributes.HTTP_REQUEST_BODY, formMap.toString());
            }
          } else {
            currentSpan.setAttribute(
                HypertraceSemanticAttributes.HTTP_REQUEST_BODY, entity.toString());
          }
        }
      }
      requestContext.getEntity();
    } catch (Exception ex) {
      log.error("Exception while getting request entity or headers", ex);
    }
  }

  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
    Object spanObj = requestContext.getProperty(ClientTracingFilter.SPAN_PROPERTY_NAME);
    if (!(spanObj instanceof Span)) {
      return;
    }

    Span currentSpan = (Span) spanObj;
    AgentConfig agentConfig = HypertraceConfig.get();

    try {
      if (agentConfig.getDataCapture().getHttpHeaders().getResponse().getValue()) {
        captureHeaders(
            currentSpan,
            HypertraceSemanticAttributes::httpResponseHeader,
            responseContext.getHeaders());
      }
    } catch (Exception ex) {
      log.error("Exception while getting response entity or headers", ex);
    }
  }

  private void captureHeaders(
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
