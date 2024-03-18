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

package io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.v1_6;

import com.google.protobuf.util.JsonFormat;
import io.grpc.ForwardingServerCall;
import io.grpc.ManagedChannel;
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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import io.opentelemetry.proto.trace.v1.Span;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.example.GreeterGrpc;
import org.hypertrace.example.GreeterGrpc.GreeterBlockingStub;
import org.hypertrace.example.Helloworld;
import org.hypertrace.example.Helloworld.Request;
import org.hypertrace.example.Helloworld.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * TODO the HTTP2 headers for client does not work for the first request - therefore the explicit
 * ordering https://github.com/hypertrace/javaagent/issues/109#issuecomment-740918018
 */
@TestMethodOrder(OrderAnnotation.class)
public class GrpcInstrumentationTest extends AbstractInstrumenterTest {

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

    CHANNEL = new GrpcChannelProvider(SERVER.getPort()).get();
  }

  @AfterAll
  public static void close() {
    CHANNEL.shutdownNow();
    SERVER.shutdownNow();
  }

  @Test
  @Order(2)
  public void blockingStub() throws IOException, TimeoutException, InterruptedException {
    Metadata headers = new Metadata();
    headers.put(CLIENT_STRING_METADATA_KEY, "clientheader");
    headers.put(BYTE_METADATA_KEY, "hello".getBytes());

    GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(CHANNEL);
    blockingStub = MetadataUtils.attachHeaders(blockingStub, headers);
    Response response = blockingStub.sayHello(REQUEST);

    String requestJson = JsonFormat.printer().print(REQUEST);
    String responseJson = JsonFormat.printer().print(response);
    List<List<Span>> traces = TEST_WRITER.waitForSpans(2);
    Assertions.assertEquals(1, traces.size());
    List<Span> spans = traces.get(0);
    Assertions.assertEquals(2, spans.size());

    Span clientSpan = spans.get(0);
    Span serverSpan = spans.get(1);
    if (spans.get(0).getKind().equals(Span.SpanKind.SPAN_KIND_SERVER)) {
      clientSpan = spans.get(1);
      serverSpan = spans.get(0);
    }
    assertBodiesAndHeaders(clientSpan, requestJson, responseJson);
    assertBodiesAndHeaders(serverSpan, requestJson, responseJson);

    assertHttp2HeadersForSayHelloMethod(serverSpan);
    assertHttp2HeadersForSayHelloMethod(clientSpan);
  }

  @Test
  @Order(1)
  public void serverRequestBlocking() throws TimeoutException, InterruptedException {
    Metadata blockHeaders = new Metadata();
    blockHeaders.put(Metadata.Key.of("mockblock", Metadata.ASCII_STRING_MARSHALLER), "true");

    GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(CHANNEL);
    blockingStub = MetadataUtils.attachHeaders(blockingStub, blockHeaders);

    try {
      Response response = blockingStub.sayHello(REQUEST);
    } catch (StatusRuntimeException ex) {
      Assertions.assertEquals(Status.PERMISSION_DENIED.getCode(), ex.getStatus().getCode());
    }

    List<List<Span>> traces = TEST_WRITER.waitForSpans(2);
    Assertions.assertEquals(1, traces.size());
    List<Span> spans = traces.get(0);
    Assertions.assertEquals(2, spans.size());

    Span serverSpan = spans.get(1);
    if (spans.get(0).getKind().equals(Span.SpanKind.SPAN_KIND_SERVER)) {
      serverSpan = spans.get(0);
    }
    Assertions.assertNull(
        TEST_WRITER.getAttributesMap(serverSpan).get("rpc.request.body"));
    Assertions.assertNull(
            TEST_WRITER.getAttributesMap(serverSpan).get("rpc.response.body"));
    Assertions.assertEquals(
        "true",
            TEST_WRITER.getAttributesMap(serverSpan)
            .get("rpc.request.metadata.mockblock").getStringValue());
    assertHttp2HeadersForSayHelloMethod(serverSpan);
  }

  private void assertBodiesAndHeaders(Span span, String requestJson, String responseJson) {
    Assertions.assertEquals(
        requestJson, TEST_WRITER.getAttributesMap(span).get("rpc.request.body").getStringValue());
    Assertions.assertEquals(
        responseJson, TEST_WRITER.getAttributesMap(span).get("rpc.response.body").getStringValue());
    Assertions.assertEquals(
        "clientheader",
        TEST_WRITER.getAttributesMap(span)
            .get("rpc.request.metadata." + CLIENT_STRING_METADATA_KEY.name()).getStringValue());
    Assertions.assertEquals(
        "serverheader",
        TEST_WRITER.getAttributesMap(span)
            .get("rpc.response.metadata." + SERVER_STRING_METADATA_KEY.name()).getStringValue());
  }

  private void assertHttp2HeadersForSayHelloMethod(Span span) {
    Assertions.assertEquals(
        "http",
        TEST_WRITER.getAttributesMap(span)
            .get("rpc.request.metadata.:scheme").getStringValue());
    Assertions.assertEquals(
        "POST",
        TEST_WRITER.getAttributesMap(span)
            .get("rpc.request.metadata.:method").getStringValue());
    Assertions.assertEquals(
        String.format("localhost:%d", SERVER.getPort()),
        TEST_WRITER.getAttributesMap(span)
            .get("rpc.request.metadata.:authority").getStringValue());
    Assertions.assertEquals(
        "/org.hypertrace.example.Greeter/SayHello",
        TEST_WRITER.getAttributesMap(span)
            .get("rpc.request.metadata.:path").getStringValue());
  }
}
