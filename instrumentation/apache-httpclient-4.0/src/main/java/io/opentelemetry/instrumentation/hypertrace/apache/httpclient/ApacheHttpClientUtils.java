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

package io.opentelemetry.instrumentation.hypertrace.apache.httpclient;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.function.Function;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpMessage;
import org.hypertrace.agent.core.ContentEncodingUtils;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApacheHttpClientUtils {
  private ApacheHttpClientUtils() {}

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

  public static void traceRequest(HttpMessage request) {
    Span currentSpan = Span.current();
    ApacheHttpClientUtils.addRequestHeaders(currentSpan, request.headerIterator());
    if (request instanceof HttpEntityEnclosingRequest) {
      HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
      HttpEntity entity = entityRequest.getEntity();
      ApacheHttpClientUtils.traceEntity(
          currentSpan, HypertraceSemanticAttributes.HTTP_REQUEST_BODY.getKey(), entity);
    }
  }

  public static void traceEntity(Span span, String bodyAttributeKey, HttpEntity entity) {
    if (entity == null) {
      return;
    }

    if (entity.isRepeatable()) {
      try {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        entity.writeTo(byteArrayOutputStream);
        String encoding =
            entity.getContentEncoding() != null ? entity.getContentEncoding().getValue() : "";

        Charset charset = ContentEncodingUtils.toCharset(encoding);
        try {
          String body = byteArrayOutputStream.toString(charset.name());
          System.out.printf("captured %s readable body is %s\n", bodyAttributeKey, body);
          span.setAttribute(bodyAttributeKey, body);
        } catch (UnsupportedEncodingException e) {
          log.error("Could not parse charset from encoding {}", encoding, e);
        }
      } catch (IOException e) {
        log.error("Could not read request input stream from repeatable request entity/body", e);
      }

      return;
    }

    ApacheHttpClientObjectRegistry.objectToSpanMap.put(entity, span);
  }
}
