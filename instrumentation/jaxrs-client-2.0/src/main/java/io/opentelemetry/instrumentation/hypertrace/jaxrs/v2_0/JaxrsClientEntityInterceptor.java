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
import io.opentelemetry.javaagent.instrumentation.jaxrsclient.v2_0.ClientTracingFilter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.ContentEncodingUtils;
import org.hypertrace.agent.core.ContentLengthUtils;
import org.hypertrace.agent.core.ContentTypeUtils;
import org.hypertrace.agent.core.GlobalObjectRegistry;
import org.hypertrace.agent.core.GlobalObjectRegistry.SpanAndBuffer;
import org.hypertrace.agent.core.HypertraceConfig;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxrsClientEntityInterceptor implements ReaderInterceptor, WriterInterceptor {

  private static final Logger log = LoggerFactory.getLogger(JaxrsClientEntityInterceptor.class);

  private static final Tracer TRACER =
      OpenTelemetry.getGlobalTracer("org.hypertrace.java.jaxrs.client");

  /** Writing response body to input stream */
  @Override
  public Object aroundReadFrom(ReaderInterceptorContext context)
      throws IOException, WebApplicationException {

    MediaType mediaType = context.getMediaType();
    AgentConfig agentConfig = HypertraceConfig.get();
    if (mediaType == null
        || !ContentTypeUtils.shouldCapture(mediaType.toString())
        || !agentConfig.getDataCapture().getHttpBody().getResponse().getValue()) {
      return context.proceed();
    }

    Object spanObj = context.getProperty(ClientTracingFilter.SPAN_PROPERTY_NAME);
    if (!(spanObj instanceof Span)) {
      log.error(
          "Span object is not present in the context properties, response object will not be captured");
      return context.proceed();
    }
    Span currentSpan = (Span) spanObj;

    InputStream entityStream = context.getInputStream();
    Object entity = null;
    try {
      String encodingStr = context.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
      String contentLengthStr = context.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
      int contentLength = ContentLengthUtils.parseLength(contentLengthStr);

      ByteArrayOutputStream buffer = new ByteArrayOutputStream(contentLength);
      GlobalObjectRegistry.inputStreamToSpanAndBufferMap.put(
          entityStream,
          new SpanAndBuffer(
              currentSpan,
              buffer,
              HypertraceSemanticAttributes.HTTP_RESPONSE_BODY,
              ContentEncodingUtils.toCharset(encodingStr)));
      entity = context.proceed();
    } catch (Exception ex) {
      log.error("Exception while capturing response body", ex);
    }
    return entity;
  }

  /** Writing request body to output stream */
  @Override
  public void aroundWriteTo(WriterInterceptorContext context)
      throws IOException, WebApplicationException {

    Object spanObj = context.getProperty(ClientTracingFilter.SPAN_PROPERTY_NAME);
    if (!(spanObj instanceof Span)) {
      log.error(
          "Span object is not present in the context properties, request body will not be captured");
      context.proceed();
      return;
    }
    Span currentSpan = (Span) spanObj;

    AgentConfig agentConfig = HypertraceConfig.get();
    if (agentConfig.getDataCapture().getHttpBody().getRequest().getValue()) {
      MediaType mediaType = context.getMediaType();
      if (mediaType == null || !ContentTypeUtils.shouldCapture(mediaType.toString())) {
        context.proceed();
        return;
      }
    }

    // TODO length is not known
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    OutputStream entityStream = context.getOutputStream();
    try {
      GlobalObjectRegistry.outputStreamToBufferMap.put(entityStream, buffer);
      context.proceed();
    } catch (Exception ex) {
      log.error("Failed to capture request body", ex);
    } finally {
      GlobalObjectRegistry.outputStreamToBufferMap.remove(entityStream);
      // TODO encoding is not known
      currentSpan.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, buffer.toString());
    }
  }
}
