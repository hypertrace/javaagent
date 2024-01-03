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

package io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.v1_6.server;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.v1_6.GrpcInstrumentationName;
import io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.v1_6.GrpcSpanDecorator;
import java.util.Map;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.filter.FilterResult;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.filter.FilterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcServerInterceptor implements ServerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(GrpcServerInterceptor.class);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    try {
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      if (!instrumentationConfig.isInstrumentationEnabled(
          GrpcInstrumentationName.PRIMARY, GrpcInstrumentationName.OTHER)) {
        return next.startCall(call, headers);
      }

      Span currentSpan = Span.current();

      Map<String, String> mapHeaders = GrpcSpanDecorator.metadataToMap(headers);

      if (instrumentationConfig.rpcMetadata().request()) {
        GrpcSpanDecorator.addMetadataAttributes(mapHeaders, currentSpan);
      }

      FilterResult filterResult =
          FilterRegistry.getFilter().evaluateRequestHeaders(currentSpan, mapHeaders);
      if (filterResult.shouldBlock()) {
        // We cannot send custom message in grpc calls
        // TODO: map http codes with grpc codes. filterResult.getBlockingStatusCode()
        call.close(Status.PERMISSION_DENIED, new Metadata());
        @SuppressWarnings("unchecked")
        ServerCall.Listener<ReqT> noop = NoopServerCallListener.INSTANCE;
        return noop;
      }

      Listener<ReqT> serverCall =
          next.startCall(new TracingServerCall<>(call, currentSpan), headers);
      return new TracingServerCallListener<>(serverCall, currentSpan);
    } catch (Throwable t) {
      log.debug("exception thrown during intercepting server call", t);
      return next.startCall(call, headers);
    }
  }

  static final class TracingServerCall<ReqT, RespT>
      extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {

    private final Span span;

    TracingServerCall(ServerCall<ReqT, RespT> delegate, Span span) {
      super(delegate);
      this.span = span;
    }

    @Override
    public void sendMessage(RespT message) {
      super.sendMessage(message);

      try {
        InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
        if (instrumentationConfig.rpcBody().response()) {
          GrpcSpanDecorator.addMessageAttribute(
              message, span, HypertraceSemanticAttributes.RPC_RESPONSE_BODY);
        }
      } catch (Throwable t) {
        log.debug("exception thrown while capturing grpc server response body", t);
      }
    }

    @Override
    public void sendHeaders(Metadata headers) {
      super.sendHeaders(headers);

      try {
        InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
        if (instrumentationConfig.rpcMetadata().response()) {
          GrpcSpanDecorator.addMetadataAttributes(
              headers, span, HypertraceSemanticAttributes::rpcResponseMetadata);
        }
      } catch (Throwable t) {
        log.debug("exception thrown while capturing grpc server response headers", t);
      }
    }
  }

  static final class TracingServerCallListener<ReqT>
      extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

    private final Span span;

    TracingServerCallListener(Listener<ReqT> delegate, Span span) {
      super(delegate);
      this.span = span;
    }

    @Override
    public void onMessage(ReqT message) {
      delegate().onMessage(message);

      try {
        InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
        if (instrumentationConfig.rpcBody().request()) {
          GrpcSpanDecorator.addMessageAttribute(
              message, span, HypertraceSemanticAttributes.RPC_REQUEST_BODY);
        }
      } catch (Throwable t) {
        log.debug("exception thrown while capturing grpc server request body", t);
      }
    }
  }
}
