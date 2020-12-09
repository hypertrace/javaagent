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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.jaxrsclient.v2_0.ClientTracingFilter;
import java.io.IOException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.ContentTypeUtils;
import org.hypertrace.agent.core.HypertraceConfig;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxrsClientEntityInterceptor implements ReaderInterceptor {

  private static final Logger log = LoggerFactory.getLogger(JaxrsClientEntityInterceptor.class);

  private static final Tracer TRACER =
      OpenTelemetry.getGlobalTracer("org.hypertrace.java.jaxrs.client");

  @Override
  public Object aroundReadFrom(ReaderInterceptorContext context)
      throws IOException, WebApplicationException {

    Object entity = context.proceed();

    Object spanObj = context.getProperty(ClientTracingFilter.SPAN_PROPERTY_NAME);
    if (!(spanObj instanceof Span)) {
      log.error(
          "Span object is not present in the context properties, response object will not be captured");
      return entity;
    }
    Span currentSpan = (Span) spanObj;

    MediaType mediaType = context.getMediaType();
    AgentConfig agentConfig = HypertraceConfig.get();
    if (mediaType == null
        || !ContentTypeUtils.shouldCapture(mediaType.toString())
        || !agentConfig.getDataCapture().getHttpBody().getResponse().getValue()) {
      return entity;
    }

    if (currentSpan.isRecording()) {
      currentSpan.setAttribute(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, entity.toString());
    } else {
      TRACER
          .spanBuilder(HypertraceSemanticAttributes.ADDITIONAL_DATA_SPAN_NAME)
          .setParent(Context.root().with(currentSpan))
          .setAttribute(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, entity.toString())
          .startSpan()
          .end();
    }

    return entity;
  }
}
