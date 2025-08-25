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

package io.opentelemetry.javaagent.instrumentation.hypertrace.okhttp.v3_0;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;
import okio.Okio;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;
import org.hypertrace.agent.core.instrumentation.utils.ServiceNameHeaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OkHttpTracingInterceptor implements Interceptor {
  private static final Logger log = LoggerFactory.getLogger(OkHttpTracingInterceptor.class);
  private static final InstrumentationConfig instrumentationConfig =
      InstrumentationConfig.ConfigProvider.get();

  @Override
  public Response intercept(Chain chain) throws IOException {
    if (!instrumentationConfig.isInstrumentationEnabled(
        Okhttp3InstrumentationName.PRIMARY, Okhttp3InstrumentationName.OTHER)) {
      return chain.proceed(chain.request());
    }

    Span span = Span.current();

    Request request = chain.request();
    if (instrumentationConfig.httpHeaders().request()) {
      captureHeaders(span, request.headers(), HypertraceSemanticAttributes::httpRequestHeader);
    }

    // Add service name header to outgoing requests
    request = addClientSeriveNameHeader(request);

    captureRequestBody(span, request.body());

    Response response = chain.proceed(request);
    if (instrumentationConfig.httpHeaders().response()) {
      captureHeaders(span, response.headers(), HypertraceSemanticAttributes::httpResponseHeader);
    }
    return captureResponseBody(span, response);
  }

  private static void captureRequestBody(Span span, RequestBody requestBody) {
    if (!instrumentationConfig.httpBody().request()) {
      return;
    }
    if (requestBody == null) {
      return;
    }
    MediaType mediaType = requestBody.contentType();
    if (mediaType == null || !ContentTypeUtils.shouldCapture(mediaType.toString())) {
      return;
    }
    try {
      Buffer buffer = new Buffer();
      requestBody.writeTo(buffer);
      span.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, buffer.readUtf8());
    } catch (IOException e) {
      log.error("Could not read request requestBody", e);
    }
  }

  private static Response captureResponseBody(Span span, final Response response) {
    if (!instrumentationConfig.httpBody().response()) {
      return response;
    }
    if (response.body() == null) {
      return response;
    }

    ResponseBody responseBody = response.body();
    MediaType mediaType = responseBody.contentType();
    if (mediaType == null || !ContentTypeUtils.shouldCapture(mediaType.toString())) {
      return response;
    }

    try {
      // Read the entire response body one-shot into a byte-array
      // responseBody.string(), this looks for the charset if available in content-type header
      // else defaults to utf-8. So read bytes itself as done here and use for building new response
      // ref: https://square.github.io/okhttp/3.x/okhttp/okhttp3/ResponseBody.html
      byte[] byteArray = responseBody.source().readByteArray();
      String body;

      // Determine the content encoding
      String contentEncoding = response.header("Content-Encoding");
      if (contentEncoding != null && contentEncoding.toLowerCase().contains("gzip")) {
        // Decompress the response body if it is GZIP encoded using GzipSource
        GzipSource gzipSource = new GzipSource(new Buffer().write(byteArray));
        BufferedSource bufferedGzipSource = Okio.buffer(gzipSource);

        // capture the decompressed content from gzip source to set as response body in span
        body = bufferedGzipSource.readString(getCharset(mediaType));
      } else {
        // capture the response body for other cases
        body = new String(byteArray, getCharset(mediaType));
      }

      span.setAttribute(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, body);

      // Return the response with its body and encoding exactly the same as the original response
      return response.newBuilder().body(ResponseBody.create(mediaType, byteArray)).build();
    } catch (IOException e) {
      log.error("Could not read response body", e);
    }

    return response;
  }

  // Helper method to determine charset from MediaType if available else default to UTF-8
  private static Charset getCharset(MediaType mediaType) {
    if (mediaType != null && mediaType.charset() != null) {
      return mediaType.charset();
    }
    return StandardCharsets.UTF_8; // Default charset
  }

  private static void captureHeaders(
      Span span, Headers headers, Function<String, AttributeKey<String>> headerNameProvider) {
    for (String name : headers.names()) {
      for (String value : headers.values(name)) {
        span.setAttribute(headerNameProvider.apply(name), value);
      }
    }
  }

  private static Request addClientSeriveNameHeader(Request request) {
    // Add service name header to outgoing requests
    request =
        request
            .newBuilder()
            .addHeader(
                ServiceNameHeaderUtils.getClientServiceKey(),
                ServiceNameHeaderUtils.getClientServiceName())
            .build();
    return request;
  }
}
