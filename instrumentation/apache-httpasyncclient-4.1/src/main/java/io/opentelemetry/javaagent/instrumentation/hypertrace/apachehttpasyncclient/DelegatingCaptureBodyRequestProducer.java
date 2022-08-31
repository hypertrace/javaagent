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
import io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient.ApacheHttpAsyncClientInstrumentation;
import io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpclient.v4_0.ApacheHttpClientUtils;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelegatingCaptureBodyRequestProducer
    extends ApacheHttpAsyncClientInstrumentation.DelegatingRequestProducer {

  private static final Logger log =
      LoggerFactory.getLogger(DelegatingCaptureBodyRequestProducer.class);

  private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
  private static final MethodHandle getContext;

  static {
    MethodHandle localGetContext;
    try {
      Field contextField =
          ApacheHttpAsyncClientInstrumentation.WrappedFutureCallback.class.getDeclaredField(
              "context");
      contextField.setAccessible(true);
      MethodHandle unReflectedField = lookup.unreflectGetter(contextField);
      localGetContext =
          unReflectedField.asType(
              MethodType.methodType(
                  Context.class, ApacheHttpAsyncClientInstrumentation.WrappedFutureCallback.class));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      log.debug("Could not find context field on super class", e);
      localGetContext = null;
    }
    getContext = localGetContext;
  }

  private final ApacheHttpAsyncClientInstrumentation.WrappedFutureCallback<?> wrappedFutureCallback;
  private final BodyCaptureDelegatingCallback<?> bodyCaptureDelegatingCallback;

  public DelegatingCaptureBodyRequestProducer(
      Context parentContext,
      HttpAsyncRequestProducer delegate,
      ApacheHttpAsyncClientInstrumentation.WrappedFutureCallback<?> wrappedFutureCallback,
      BodyCaptureDelegatingCallback<?> bodyCaptureDelegatingCallback) {
    super(parentContext, delegate, wrappedFutureCallback);
    this.wrappedFutureCallback = wrappedFutureCallback;
    this.bodyCaptureDelegatingCallback = bodyCaptureDelegatingCallback;
  }

  @Override
  public HttpRequest generateRequest() throws IOException, HttpException {
    HttpRequest request = super.generateRequest();
    try {
      /*
       * Ideally, we should not rely on the getContext MethodHandle here. The client context isn't
       * generated until super.generateRequest is called so ideally we should architect our code
       * such that we can access the client span more idiomatically after it is created, like via
       * the Java8BytecodeBridge.currentSpan call.
       */
      Object getContextResult = getContext.invoke(wrappedFutureCallback);
      if (getContextResult instanceof Context) {
        Context context = (Context) getContextResult;
        Span clientSpan = Span.fromContextOrNull(context);
        bodyCaptureDelegatingCallback.setClientContext(context);
        ApacheHttpClientUtils.traceRequest(clientSpan, request);
      }
    } catch (Throwable t) {
      log.debug("Could not access context field on super class", t);
    }
    return request;
  }
}
