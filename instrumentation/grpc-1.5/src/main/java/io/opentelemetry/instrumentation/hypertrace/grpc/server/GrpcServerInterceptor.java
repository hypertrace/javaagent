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

package io.opentelemetry.instrumentation.hypertrace.grpc.server;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.opentelemetry.instrumentation.hypertrace.grpc.GrpcSpanDecorator;
import io.opentelemetry.instrumentation.hypertrace.grpc.GrpcTracer;
import io.opentelemetry.instrumentation.hypertrace.grpc.server.GrpcServerInterceptor.TracingServerCall.TracingServerCallListener;
import io.opentelemetry.trace.Span;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;

public class GrpcServerInterceptor implements ServerInterceptor {

  private static final GrpcTracer TRACER = new GrpcTracer();

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    Span currentSpan = TRACER.getCurrentSpan();
    GrpcSpanDecorator.addMetadataAttributes(
        headers, currentSpan, HypertraceSemanticAttributes::rpcRequestMetadata);

    return new TracingServerCallListener<>(
        next.startCall(new GrpcServerInterceptor.TracingServerCall<>(call, currentSpan), headers),
        currentSpan);
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
      GrpcSpanDecorator.addMessageAttribute(
          message, span, HypertraceSemanticAttributes.RPC_RESPONSE_BODY);
      super.sendMessage(message);
    }

    @Override
    public void sendHeaders(Metadata headers) {
      GrpcSpanDecorator.addMetadataAttributes(
          headers, span, HypertraceSemanticAttributes::rpcResponseMetadata);
      super.sendHeaders(headers);
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
        GrpcSpanDecorator.addMessageAttribute(
            message, span, HypertraceSemanticAttributes.RPC_REQUEST_BODY);
        delegate().onMessage(message);
      }
    }
  }
}
