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

package io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpasyncclient;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpclient.v4_0.ApacheHttpClientUtils;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

public class BodyCaptureDelegatingCallback<T> implements FutureCallback<T> {

  final Context context;
  final HttpContext httpContext;
  private final FutureCallback<T> delegate;
  private volatile Context clientContext;

  public BodyCaptureDelegatingCallback(
      Context context, HttpContext httpContext, FutureCallback<T> delegate) {
    this.context = context;
    this.httpContext = httpContext;
    this.delegate = delegate;
  }

  @Override
  public void completed(T result) {
    HttpResponse httpResponse = getResponse(httpContext);
    ApacheHttpClientUtils.traceResponse(Span.fromContext(clientContext), httpResponse);
    delegate.completed(result);
  }

  @Override
  public void failed(Exception ex) {
    HttpResponse httpResponse = getResponse(httpContext);
    ApacheHttpClientUtils.traceResponse(Span.fromContext(clientContext), httpResponse);
    delegate.failed(ex);
  }

  @Override
  public void cancelled() {
    HttpResponse httpResponse = getResponse(httpContext);
    ApacheHttpClientUtils.traceResponse(Span.fromContext(clientContext), httpResponse);
    delegate.cancelled();
  }

  private static HttpResponse getResponse(HttpContext context) {
    return (HttpResponse) context.getAttribute(HttpCoreContext.HTTP_RESPONSE);
  }

  public void setClientContext(Context clientContext) {
    this.clientContext = clientContext;
  }
}
