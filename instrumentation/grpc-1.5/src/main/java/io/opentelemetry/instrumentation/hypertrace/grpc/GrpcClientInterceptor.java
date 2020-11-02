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

package io.opentelemetry.instrumentation.hypertrace.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientCall.Listener;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;
import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.trace.Span;
import java.util.function.Function;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcClientInterceptor implements ClientInterceptor {
  private static final Logger log = LoggerFactory.getLogger(GrpcClientInterceptor.class);

  private static final JsonFormat.Printer PRINTER = JsonFormat.printer();
  private static final GrpcClientTracer TRACER = new GrpcClientTracer();

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

    Span currentSpan = TRACER.getCurrentSpan();
    ClientCall<ReqT, RespT> result = next.newCall(method, callOptions);
    return new GrpcClientInterceptor.TracingClientCall<>(result, currentSpan, TRACER);
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
      addMetadataAttributes(headers, span, HypertraceSemanticAttributes::rpcRequestMetadata);
      super.start(new TracingClientCallListener<>(responseListener, span), headers);
    }

    @Override
    public void sendMessage(ReqT message) {
      addMessageAttribute(message, span, HypertraceSemanticAttributes.RPC_REQUEST_BODY);
      super.sendMessage(message);
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
      addMessageAttribute(message, span, HypertraceSemanticAttributes.RPC_RESPONSE_BODY);
      delegate().onMessage(message);
    }

    @Override
    public void onHeaders(Metadata headers) {
      addMetadataAttributes(headers, span, HypertraceSemanticAttributes::rpcResponseMetadata);
      super.onHeaders(headers);
    }
  }

  private static void addMessageAttribute(Object message, Span span, AttributeKey<String> key) {
    if (message instanceof Message) {
      Message mb = (Message) message;
      try {
        String requestBody = PRINTER.print(mb);
        span.setAttribute(key, requestBody);
      } catch (InvalidProtocolBufferException e) {
        log.error("Failed to parse request message to JSON", e);
      }
    }
  }

  private static void addMetadataAttributes(
      Metadata metadata, Span span, Function<String, AttributeKey<String>> keySupplier) {
    for (String key : metadata.keys()) {
      if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
        // do not add binary metadata
        continue;
      }
      Key<String> stringKey = Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
      Iterable<String> stringValues = metadata.getAll(stringKey);
      for (String stringValue : stringValues) {
        span.setAttribute(keySupplier.apply(key), stringValue);
      }
      System.out.printf("requestMetadata: %s=%s\n", key, stringValues);
    }
  }
}
