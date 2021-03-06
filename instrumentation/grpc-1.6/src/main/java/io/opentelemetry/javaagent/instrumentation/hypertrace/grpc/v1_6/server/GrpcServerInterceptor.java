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
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.filter.FilterRegistry;

public class GrpcServerInterceptor implements ServerInterceptor {

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

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

    boolean block = FilterRegistry.getFilter().evaluateRequestHeaders(currentSpan, mapHeaders);
    if (block) {
      call.close(Status.PERMISSION_DENIED, new Metadata());
      @SuppressWarnings("unchecked")
      ServerCall.Listener<ReqT> noop = NoopServerCallListener.INSTANCE;
      return noop;
    }

    Listener<ReqT> serverCall = next.startCall(new TracingServerCall<>(call, currentSpan), headers);
    return new TracingServerCallListener<>(serverCall, currentSpan);
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

      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      if (instrumentationConfig.rpcBody().response()) {
        GrpcSpanDecorator.addMessageAttribute(
            message, span, HypertraceSemanticAttributes.RPC_RESPONSE_BODY);
      }
    }

    @Override
    public void sendHeaders(Metadata headers) {
      super.sendHeaders(headers);

      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      if (instrumentationConfig.rpcMetadata().response()) {
        GrpcSpanDecorator.addMetadataAttributes(
            headers, span, HypertraceSemanticAttributes::rpcResponseMetadata);
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

      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      if (instrumentationConfig.rpcBody().request()) {
        GrpcSpanDecorator.addMessageAttribute(
            message, span, HypertraceSemanticAttributes.RPC_REQUEST_BODY);
      }
    }
  }
}
