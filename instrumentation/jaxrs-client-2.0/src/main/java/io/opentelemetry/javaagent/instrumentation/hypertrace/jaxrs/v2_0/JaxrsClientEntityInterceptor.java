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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.jaxrsclient.v2_0.ClientTracingFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.bootstrap.config.HypertraceConfig;
import org.hypertrace.agent.core.bootstrap.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.bootstrap.instrumentation.SpanAndBuffer;
import org.hypertrace.agent.core.bootstrap.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.bootstrap.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.bootstrap.instrumentation.utils.ContentLengthUtils;
import org.hypertrace.agent.core.bootstrap.instrumentation.utils.ContentTypeCharsetUtils;
import org.hypertrace.agent.core.bootstrap.instrumentation.utils.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxrsClientEntityInterceptor implements ReaderInterceptor, WriterInterceptor {

  private static final Logger log = LoggerFactory.getLogger(JaxrsClientEntityInterceptor.class);

  private final ContextStore<InputStream, SpanAndBuffer> inputStreamContextStore;
  private final ContextStore<OutputStream, BoundedByteArrayOutputStream> outputStreamContextStore;

  public JaxrsClientEntityInterceptor(
      ContextStore<InputStream, SpanAndBuffer> inputStreamContextStore,
      ContextStore<OutputStream, BoundedByteArrayOutputStream> outputStreamContextStore) {
    this.inputStreamContextStore = inputStreamContextStore;
    this.outputStreamContextStore = outputStreamContextStore;
  }

  /** Writing response body to input stream */
  @Override
  public Object aroundReadFrom(ReaderInterceptorContext responseContext)
      throws IOException, WebApplicationException {

    MediaType mediaType = responseContext.getMediaType();
    AgentConfig agentConfig = HypertraceConfig.get();
    if (mediaType == null
        || !ContentTypeUtils.shouldCapture(mediaType.toString())
        || !agentConfig.getDataCapture().getHttpBody().getResponse().getValue()) {
      return responseContext.proceed();
    }

    Object contextObj = responseContext.getProperty(ClientTracingFilter.CONTEXT_PROPERTY_NAME);
    if (!(contextObj instanceof Context)) {
      log.error(
          "Span object is not present in the context properties, response object will not be captured");
      return responseContext.proceed();
    }
    Context context = (Context) contextObj;
    Span currentSpan = Span.fromContext(context);

    // TODO as optimization the type could be checked here and if it is a primitive type e.g. String
    // it could be read directly.
    //    context.getType();

    InputStream entityStream = responseContext.getInputStream();
    Object entity = null;
    try {
      String contentLengthStr = responseContext.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
      int contentLength = ContentLengthUtils.parseLength(contentLengthStr);

      String charsetStr = null;
      if (mediaType != null) {
        charsetStr = mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);
      }
      Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);

      BoundedByteArrayOutputStream buffer =
          BoundedBuffersFactory.createStream(contentLength, charset);

      inputStreamContextStore.put(
          entityStream,
          new SpanAndBuffer(
              currentSpan, buffer, HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, charset));

      entity = responseContext.proceed();
    } catch (Exception ex) {
      log.error("Exception while capturing response body", ex);
    }
    return entity;
  }

  /** Writing request body to output stream */
  @Override
  public void aroundWriteTo(WriterInterceptorContext requestContext)
      throws IOException, WebApplicationException {

    Object contextObj = requestContext.getProperty(ClientTracingFilter.CONTEXT_PROPERTY_NAME);
    if (!(contextObj instanceof Context)) {
      log.error(
          "Span object is not present in the context properties, request body will not be captured");
      requestContext.proceed();
      return;
    }
    Context context = (Context) contextObj;
    Span currentSpan = Span.fromContext(context);

    AgentConfig agentConfig = HypertraceConfig.get();
    if (agentConfig.getDataCapture().getHttpBody().getRequest().getValue()) {
      MediaType mediaType = requestContext.getMediaType();
      if (mediaType == null || !ContentTypeUtils.shouldCapture(mediaType.toString())) {
        requestContext.proceed();
        return;
      }
    }

    MediaType mediaType = requestContext.getMediaType();
    String charsetStr = null;
    if (mediaType != null) {
      charsetStr = mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);
    }
    Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);

    // TODO length is not known
    BoundedByteArrayOutputStream buffer = BoundedBuffersFactory.createStream(charset);
    OutputStream entityStream = requestContext.getOutputStream();
    try {
      outputStreamContextStore.put(entityStream, buffer);
      requestContext.proceed();
    } catch (Exception ex) {
      log.error("Failed to capture request body", ex);
    } finally {
      outputStreamContextStore.put(entityStream, null);
      currentSpan.setAttribute(
          HypertraceSemanticAttributes.HTTP_REQUEST_BODY, buffer.toStringWithSuppliedCharset());
    }
  }
}
