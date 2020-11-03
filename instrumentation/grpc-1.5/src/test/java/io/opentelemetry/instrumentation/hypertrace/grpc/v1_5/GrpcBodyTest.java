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

package io.opentelemetry.instrumentation.hypertrace.grpc.v1_5;

import com.google.protobuf.util.JsonFormat;
import io.grpc.ForwardingServerCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.hypertrace.agent.core.DynamicConfig;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.example.GreeterGrpc;
import org.hypertrace.example.GreeterGrpc.GreeterBlockingStub;
import org.hypertrace.example.Helloworld;
import org.hypertrace.example.Helloworld.Request;
import org.hypertrace.example.Helloworld.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GrpcBodyTest extends AbstractInstrumenterTest {

  private static final Helloworld.Request REQUEST =
      Request.newBuilder().setName("request name").build();
  private static final Metadata.Key<String> SERVER_STRING_METADATA_KEY =
      Metadata.Key.of("serverheaderkey", Metadata.ASCII_STRING_MARSHALLER);
  private static final Metadata.Key<String> CLIENT_STRING_METADATA_KEY =
      Metadata.Key.of("clientheaderkey", Metadata.ASCII_STRING_MARSHALLER);
  private static final Metadata.Key<byte[]> BYTE_METADATA_KEY =
      Metadata.Key.of("name" + Metadata.BINARY_HEADER_SUFFIX, Metadata.BINARY_BYTE_MARSHALLER);

  private static Server SERVER;
  private static ManagedChannel CHANNEL;

  @BeforeAll
  public static void startServer() throws IOException {
    SERVER =
        ServerBuilder.forPort(0)
            .addService(new NoopGreeterService())
            .intercept(
                new ServerInterceptor() {
                  @Override
                  public <ReqT, RespT> Listener<ReqT> interceptCall(
                      ServerCall<ReqT, RespT> serverCall,
                      Metadata metadata,
                      ServerCallHandler<ReqT, RespT> serverCallHandler) {
                    ServerCall<ReqT, RespT> serverCall2 =
                        new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(
                            serverCall) {
                          @Override
                          public void sendHeaders(Metadata headers) {
                            Metadata addHeaders = new Metadata();
                            addHeaders.put(SERVER_STRING_METADATA_KEY, "serverheader");
                            addHeaders.put(BYTE_METADATA_KEY, "serverbyteheader".getBytes());
                            headers.merge(addHeaders);
                            super.sendHeaders(headers);
                          }
                        };

                    return serverCallHandler.startCall(serverCall2, metadata);
                  }
                })
            .build();
    SERVER.start();

    CHANNEL =
        ManagedChannelBuilder.forTarget(String.format("localhost:%d", SERVER.getPort()))
            .usePlaintext(true)
            .build();
  }

  @AfterAll
  public static void close() {
    CHANNEL.shutdownNow();
    SERVER.shutdownNow();
  }

  @AfterEach
  public void afterEach() {
    System.clearProperty(DynamicConfig.ENABLED_ALL_PROPERTY_NAME);
  }

  @Test
  public void blockingStub() throws IOException, TimeoutException, InterruptedException {
    Metadata headers = new Metadata();
    headers.put(CLIENT_STRING_METADATA_KEY, "clientheader");
    headers.put(BYTE_METADATA_KEY, "hello".getBytes());

    GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(CHANNEL);
    blockingStub = MetadataUtils.attachHeaders(blockingStub, headers);
    Response response = blockingStub.sayHello(REQUEST);

    String requestJson = JsonFormat.printer().print(REQUEST);
    String responseJson = JsonFormat.printer().print(response);

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    List<SpanData> spans = traces.get(0);
    Assertions.assertEquals(2, spans.size());

    SpanData clientSpan = spans.get(0);
    assertBodiesAndHeaders(clientSpan, requestJson, responseJson);
    SpanData serverSpan = spans.get(1);
    assertBodiesAndHeaders(serverSpan, requestJson, responseJson);
  }

  @Test
  public void serverRequestBlocking() throws TimeoutException, InterruptedException {
    Metadata blockHeaders = new Metadata();
    blockHeaders.put(Metadata.Key.of("block", Metadata.ASCII_STRING_MARSHALLER), "true");

    GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(CHANNEL);
    blockingStub = MetadataUtils.attachHeaders(blockingStub, blockHeaders);

    try {
      Response response = blockingStub.sayHello(REQUEST);
    } catch (StatusRuntimeException ex) {
      Assertions.assertEquals(Status.PERMISSION_DENIED, ex.getStatus());
    }

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    List<SpanData> spans = traces.get(0);
    Assertions.assertEquals(2, spans.size());

    SpanData serverSpan = spans.get(1);
    Assertions.assertNull(
        serverSpan.getAttributes().get(HypertraceSemanticAttributes.RPC_REQUEST_BODY));
    Assertions.assertNull(
        serverSpan.getAttributes().get(HypertraceSemanticAttributes.RPC_RESPONSE_BODY));
    Assertions.assertEquals(
        "true",
        serverSpan.getAttributes().get(HypertraceSemanticAttributes.rpcRequestMetadata("block")));
  }

  @Test
  public void disabledInstrumentation_dynamicConfig()
      throws TimeoutException, InterruptedException {
    System.setProperty(DynamicConfig.ENABLED_ALL_PROPERTY_NAME, "false");

    GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(CHANNEL);
    Response response = blockingStub.sayHello(REQUEST);

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    List<SpanData> spans = traces.get(0);
    Assertions.assertEquals(2, spans.size());

    SpanData clientSpan = spans.get(0);
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.RPC_REQUEST_BODY));
    Assertions.assertNull(
        clientSpan.getAttributes().get(HypertraceSemanticAttributes.RPC_RESPONSE_BODY));
    SpanData serverSpan = spans.get(1);
    Assertions.assertNull(
        serverSpan.getAttributes().get(HypertraceSemanticAttributes.RPC_REQUEST_BODY));
    Assertions.assertNull(
        serverSpan.getAttributes().get(HypertraceSemanticAttributes.RPC_RESPONSE_BODY));
  }

  private void assertBodiesAndHeaders(SpanData span, String requestJson, String responseJson) {
    Assertions.assertEquals(
        requestJson, span.getAttributes().get(HypertraceSemanticAttributes.RPC_REQUEST_BODY));
    Assertions.assertEquals(
        responseJson, span.getAttributes().get(HypertraceSemanticAttributes.RPC_RESPONSE_BODY));
    Assertions.assertEquals(
        "clientheader",
        span.getAttributes()
            .get(
                HypertraceSemanticAttributes.rpcRequestMetadata(
                    CLIENT_STRING_METADATA_KEY.name())));
    Assertions.assertEquals(
        "serverheader",
        span.getAttributes()
            .get(
                HypertraceSemanticAttributes.rpcResponseMetadata(
                    SERVER_STRING_METADATA_KEY.name())));
  }
}
