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

package io.opentelemetry.instrumentation.hypertrace.okhttp.v3_0;

import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.javaagent.instrumentation.okhttp.v3_0.OkHttpClientTracer;
import io.opentelemetry.trace.Span;
import java.io.IOException;
import java.util.function.Function;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.hypertrace.agent.core.ContentTypeUtils;
import org.hypertrace.agent.core.HypertraceConfig;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OkHttpTracingInterceptor implements Interceptor {
  private static final Logger log = LoggerFactory.getLogger(OkHttpTracingInterceptor.class);

  private static final OkHttpClientTracer TRACER = OkHttpClientTracer.TRACER;

  @Override
  public Response intercept(Chain chain) throws IOException {
    if (!HypertraceConfig.isInstrumentationEnabled(InstrumentationName.INSTRUMENTATION_NAME)) {
      return chain.proceed(chain.request());
    }

    Span span = TRACER.getCurrentSpan();

    Request request = chain.request();
    if (HypertraceConfig.get().getDataCapture().getHttpHeaders().getRequest().getValue()) {
      captureHeaders(span, request.headers(), HypertraceSemanticAttributes::httpRequestHeader);
    }
    captureRequestBody(span, request.body());

    Response response = chain.proceed(request);
    if (HypertraceConfig.get().getDataCapture().getHttpHeaders().getResponse().getValue()) {
      captureHeaders(span, response.headers(), HypertraceSemanticAttributes::httpResponseHeader);
    }
    return captureResponseBody(span, response);
  }

  private static void captureRequestBody(Span span, RequestBody requestBody) {
    if (!HypertraceConfig.get().getDataCapture().getHttpBody().getRequest().getValue()) {
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
    if (!HypertraceConfig.get().getDataCapture().getHttpBody().getResponse().getValue()) {
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
      String body = responseBody.string();
      span.setAttribute(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY, body);
      return response
          .newBuilder()
          .body(ResponseBody.create(responseBody.contentType(), body))
          .build();
    } catch (IOException e) {
      log.error("Could not read response body", e);
    }
    return response;
  }

  private static void captureHeaders(
      Span span, Headers headers, Function<String, AttributeKey<String>> headerNameProvider) {
    for (String name : headers.names()) {
      for (String value : headers.values(name)) {
        span.setAttribute(headerNameProvider.apply(name), value);
      }
    }
  }
}
