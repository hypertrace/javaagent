package io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1;

import io.netty.handler.codec.http.HttpServerCodec;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class Netty41ServerDuplexCodecInstrumentationTest extends Netty41ServerInstrumentationTest {

  private static int port;
  private static NettyTestServer nettyTestServer;

  @BeforeAll
  private static void startServer() throws IOException, InterruptedException {
    nettyTestServer = new NettyTestServer();
    port = nettyTestServer.create(Arrays.asList(HttpServerCodec.class));
  }

  @AfterAll
  private static void stopServer() {
    nettyTestServer.stopServer();
  }
}
