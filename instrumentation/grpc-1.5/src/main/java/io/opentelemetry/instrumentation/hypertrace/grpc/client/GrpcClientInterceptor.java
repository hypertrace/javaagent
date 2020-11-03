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

package io.opentelemetry.instrumentation.hypertrace.grpc.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientCall.Listener;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.opentelemetry.instrumentation.hypertrace.grpc.GrpcSpanDecorator;
import io.opentelemetry.instrumentation.hypertrace.grpc.GrpcTracer;
import io.opentelemetry.trace.Span;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;

public class GrpcClientInterceptor implements ClientInterceptor {

  private static final GrpcTracer TRACER = new GrpcTracer();

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

    Span currentSpan = TRACER.getCurrentSpan();
    ClientCall<ReqT, RespT> clientCall = next.newCall(method, callOptions);
    return new GrpcClientInterceptor.TracingClientCall<>(clientCall, currentSpan, TRACER);
  }

  static final class TracingClientCall<ReqT, RespT>
      extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

    private final Span span;

    TracingClientCall(
        ClientCall<ReqT, RespT> delegate,
        Span span,
        io.opentelemetry.instrumentation.grpc.v1_5.client.GrpcClientTracer tracer) {
      super(delegate);
      this.span = span;
    }

    @Override
    public void start(Listener<RespT> responseListener, Metadata headers) {
      super.start(new TracingClientCallListener<>(responseListener, span), headers);
      GrpcSpanDecorator.addMetadataAttributes(
          headers, span, HypertraceSemanticAttributes::rpcRequestMetadata);
    }

    @Override
    public void sendMessage(ReqT message) {
      super.sendMessage(message);
      GrpcSpanDecorator.addMessageAttribute(
          message, span, HypertraceSemanticAttributes.RPC_REQUEST_BODY);
    }
  }

  static final class TracingClientCallListener<RespT>
      extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {
    private final Span span;

    TracingClientCallListener(Listener<RespT> delegate, Span span) {
      super(delegate);
      this.span = span;
    }

    @Override
    public void onMessage(RespT message) {
      delegate().onMessage(message);
      GrpcSpanDecorator.addMessageAttribute(
          message, span, HypertraceSemanticAttributes.RPC_RESPONSE_BODY);
    }

    @Override
    public void onHeaders(Metadata headers) {
      super.onHeaders(headers);
      GrpcSpanDecorator.addMetadataAttributes(
          headers, span, HypertraceSemanticAttributes::rpcResponseMetadata);
    }
  }
}
