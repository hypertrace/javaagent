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

package io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.v1_6.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientCall.Listener;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.v1_6.GrpcInstrumentationName;
import io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.v1_6.GrpcSpanDecorator;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.utils.ServiceNameHeaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcClientInterceptor implements ClientInterceptor {

  private static final Metadata.Key<String> SERVICE_NAME_METADATA_KEY =
      Metadata.Key.of(
          ServiceNameHeaderUtils.getClientServiceKey(), Metadata.ASCII_STRING_MARSHALLER);

  private static final Logger log = LoggerFactory.getLogger(GrpcClientInterceptor.class);

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

    try {
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      if (!instrumentationConfig.isInstrumentationEnabled(
          GrpcInstrumentationName.PRIMARY, GrpcInstrumentationName.OTHER)) {
        return next.newCall(method, callOptions);
      }

      Span currentSpan = Span.current();
      ClientCall<ReqT, RespT> clientCall = next.newCall(method, callOptions);
      return new GrpcClientInterceptor.TracingClientCall<>(clientCall, currentSpan);
    } catch (Throwable t) {
      log.debug("exception thrown while intercepting grpc client call", t);
      return next.newCall(method, callOptions);
    }
  }

  static final class TracingClientCall<ReqT, RespT>
      extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

    private final Span span;

    TracingClientCall(ClientCall<ReqT, RespT> delegate, Span span) {
      super(delegate);
      this.span = span;
    }

    @Override
    public void start(Listener<RespT> responseListener, Metadata headers) {
      // Add service name header to outgoing requests
      headers.put(SERVICE_NAME_METADATA_KEY, ServiceNameHeaderUtils.getClientServiceName());

      super.start(new TracingClientCallListener<>(responseListener, span), headers);

      try {
        InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
        if (instrumentationConfig.rpcMetadata().request()) {
          GrpcSpanDecorator.addMetadataAttributes(
              headers, span, HypertraceSemanticAttributes::rpcRequestMetadata);
        }
      } catch (Throwable t) {
        log.debug("exception thrown while capturing grpc client request metadata", t);
      }
    }

    @Override
    public void sendMessage(ReqT message) {
      super.sendMessage(message);

      try {
        InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
        if (instrumentationConfig.rpcBody().request()) {
          GrpcSpanDecorator.addMessageAttribute(
              message, span, HypertraceSemanticAttributes.RPC_REQUEST_BODY);
        }
      } catch (Throwable t) {
        log.debug("exception thrown while capturing grpc client request body", t);
      }
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

      try {
        InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
        if (instrumentationConfig.rpcBody().response()) {
          GrpcSpanDecorator.addMessageAttribute(
              message, span, HypertraceSemanticAttributes.RPC_RESPONSE_BODY);
        }
      } catch (Throwable t) {
        log.debug("exception thrown while capturing grpc client response body", t);
      }
    }

    @Override
    public void onHeaders(Metadata headers) {
      super.onHeaders(headers);

      try {
        InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
        if (instrumentationConfig.rpcMetadata().response()) {
          GrpcSpanDecorator.addMetadataAttributes(
              headers, span, HypertraceSemanticAttributes::rpcResponseMetadata);
        }
      } catch (Throwable t) {
        log.debug("exception thrown while capturing grpc client response metadata", t);
      }
    }
  }
}
