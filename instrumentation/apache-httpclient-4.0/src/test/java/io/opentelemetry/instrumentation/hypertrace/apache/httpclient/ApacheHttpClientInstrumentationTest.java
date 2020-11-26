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

package io.opentelemetry.instrumentation.hypertrace.apache.httpclient;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.hypertrace.agent.testing.TestHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ApacheHttpClientInstrumentationTest extends AbstractInstrumenterTest {

  private static final TestHttpServer testHttpServer = new TestHttpServer();

  @BeforeAll
  public static void startServer() throws Exception {
    testHttpServer.start();
  }

  @AfterAll
  public static void closeServer() throws Exception {
    testHttpServer.close();
  }

  @Test
  public void test() throws IOException, TimeoutException, InterruptedException {
    HttpClient client = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet();
    getRequest.setURI(
        URI.create(String.format("http://localhost:%d/get_json", testHttpServer.port())));
    HttpResponse response = client.execute(getRequest);

    // TODO add test when a different span is active
    System.out.println(response.getEntity());
    InputStream inputStream = response.getEntity().getContent();

    // returns exception
    //    InputStream inputStream2 = response.getEntity().getContent();
    ByteBuffer buff = ByteBuffer.allocate(100);
    byte ch;
    while ((ch = (byte) inputStream.read()) != -1) {
      buff.put(ch);
    }
    Assertions.assertEquals("{\"name\": \"james\"}", new String(buff.array()));
    System.out.println("test read body");
    System.out.println(new String(buff.array()));

    Assertions.assertEquals(200, response.getStatusLine().getStatusCode());

    TEST_WRITER.waitForTraces(1);
    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    Assertions.assertEquals(1, traces.size());
    Assertions.assertEquals(1, traces.get(0).size());
    SpanData span = traces.get(0).get(0);
    System.out.println(span);
  }

  private String readInputStream(InputStream inputStream) throws IOException {
    StringBuilder textBuilder = new StringBuilder();

    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
      int c = 0;
      // or reader.readLine()
      while ((c = reader.read()) != -1) {
        textBuilder.append((char) c);
      }
    }
    return textBuilder.toString();
  }
}
