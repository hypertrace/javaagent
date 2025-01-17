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

package io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpclient.v4_0;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpclient.v4_0.ApacheHttpClientObjectRegistry.SpanAndAttributeKey;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.config.InstrumentationConfig.ConfigProvider;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeCharsetUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApacheHttpClientUtils {
  private ApacheHttpClientUtils() {}

  private static InstrumentationConfig instrumentationConfig = ConfigProvider.get();

  private static final Logger log = LoggerFactory.getLogger(ApacheHttpClientUtils.class);

  public static void addResponseHeaders(Span span, HeaderIterator headerIterator) {
    addHeaders(span, headerIterator, HypertraceSemanticAttributes::httpResponseHeader);
  }

  public static void addRequestHeaders(Span span, HeaderIterator headerIterator) {
    addHeaders(span, headerIterator, HypertraceSemanticAttributes::httpRequestHeader);
  }

  private static void addHeaders(
      Span span,
      HeaderIterator headerIterator,
      Function<String, AttributeKey<String>> attributeKeySupplier) {
    while (headerIterator.hasNext()) {
      Header header = headerIterator.nextHeader();
      span.setAttribute(attributeKeySupplier.apply(header.getName()), header.getValue());
    }
  }

  public static void traceRequest(Span span, HttpMessage request) {
    if (instrumentationConfig.httpHeaders().request()) {
      ApacheHttpClientUtils.addRequestHeaders(span, request.headerIterator());
    }

    if (instrumentationConfig.httpBody().request()
        && request instanceof HttpEntityEnclosingRequest) {
      HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
      HttpEntity entity = entityRequest.getEntity();
      ApacheHttpClientUtils.traceEntity(
          span, HypertraceSemanticAttributes.HTTP_REQUEST_BODY, entity);
    }
  }

  public static void traceResponse(Span span, HttpResponse response) {
    if (instrumentationConfig.httpHeaders().response()) {
      ApacheHttpClientUtils.addResponseHeaders(span, response.headerIterator());
    }

    if (instrumentationConfig.httpBody().response()) {
      HttpEntity entity = response.getEntity();
      ApacheHttpClientUtils.traceEntity(
          span, HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, entity);
    }
  }

  public static void traceEntity(
      Span span, AttributeKey<String> bodyAttributeKey, HttpEntity entity) {
    if (entity == null) {
      return;
    }

    Header contentType = entity.getContentType();
    if (contentType == null || !ContentTypeUtils.shouldCapture(contentType.getValue())) {
      return;
    }
    String charsetStr = ContentTypeUtils.parseCharset(contentType.getValue());
    Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);
    // Get the content encoding header and check if it's gzip
    Header contentEncoding = entity.getContentEncoding();
    boolean isGzipEncoded =
        contentEncoding != null
            && contentEncoding.getValue() != null
            && contentEncoding.getValue().toLowerCase().contains("gzip");
    if (entity.isRepeatable()) {
      try {
        BoundedByteArrayOutputStream byteArrayOutputStream =
            BoundedBuffersFactory.createStream(charset);
        entity.writeTo(byteArrayOutputStream);
        InputStream contentStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        if (isGzipEncoded) {
          try {
            contentStream = new GZIPInputStream(contentStream);
          } catch (IOException e) {
            log.error("Failed to create GZIPInputStream", e);
            return;
          }
        }

        String body = readInputStream(contentStream, charset);
        span.setAttribute(bodyAttributeKey, body);

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return;
    }

    // sync client: request body is traced via HttpEntity.writeTo(OutputStream) and OutputStream
    // instrumentation
    // async client: request body is traced via InputStream HttpEntity.getContent() and InputStream
    // instrumentation

    // response body is traced via InputStream HttpEntity.getContent() and InputStream
    // instrumentation
    ApacheHttpClientObjectRegistry.entityToSpan.put(
        entity, new SpanAndAttributeKey(span, bodyAttributeKey));
  }

  public static String readInputStream(InputStream inputStream, Charset charset)
      throws IOException {
    BoundedByteArrayOutputStream outputStream = BoundedBuffersFactory.createStream(charset);
    try (InputStreamReader reader = new InputStreamReader(inputStream, charset);
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, charset)) {
      int c;
      while ((c = reader.read()) != -1) {
        writer.write(c);
      }
      writer.flush();
    }
    return outputStream.toStringWithSuppliedCharset();
  }
}
