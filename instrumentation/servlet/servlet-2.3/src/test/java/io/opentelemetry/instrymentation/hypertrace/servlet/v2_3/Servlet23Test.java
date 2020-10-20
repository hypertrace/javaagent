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

package io.opentelemetry.instrymentation.hypertrace.servlet.v2_3;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.jupiter.api.Test;

public class Servlet23Test extends AbstractAgentTest {

  @Test
  public void simpleServlet() throws Exception {
    Server server = new Server(0);
    ServletContextHandler handler = new ServletContextHandler();
    handler.addServlet(TestServlet.class, "/test");

    server.setHandler(handler);
    server.start();

    int serverPort = server.getConnectors()[0].getLocalPort();

    OkHttpClient httpClient = new Builder().build();
    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/test", serverPort))
            .get()
            .build();
    Response response = httpClient.newCall(request).execute();

    server.stop();
  }
}
