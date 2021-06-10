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

package io.opentelemetry.javaagent.instrumentation.hypertrace.undertow.v1_4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class UndertowInstrumentationTest extends AbstractInstrumenterTest {

  @Test
  void testCaptureRequestBody() throws Exception {
    final int availablePort;
    try (ServerSocket socket = new ServerSocket(0)) {
      availablePort = socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    final String contextPath = "/myapp";
    final DeploymentInfo deploymentInfo =
        Servlets.deployment()
            .setClassLoader(TestServlet.class.getClassLoader())
            .setContextPath(contextPath)
            .setDeploymentName("myapp.war")
            .addServlet(Servlets.servlet("TestServlet", TestServlet.class).addMapping("/*"));
    final DeploymentManager deploymentManager =
        Servlets.defaultContainer().addDeployment(deploymentInfo);
    deploymentManager.deploy();
    final HttpHandler servletHandler = deploymentManager.start();
    final PathHandler rootHandler =
        Handlers.path(Handlers.redirect(contextPath)).addPrefixPath(contextPath, servletHandler);

    final Undertow server =
        Undertow.builder()
            .addHttpListener(availablePort, "localhost")
            .setHandler(rootHandler)
            .build();
    server.start();

    final String requestBody = "echo=bar";
    final Request request =
        new Builder()
            .url("http://localhost:" + availablePort + "/myapp/")
            .post(
                RequestBody.create(requestBody, MediaType.get("application/x-www-form-urlencoded")))
            .build();

    try (Response response = this.httpClient.newCall(request).execute()) {
      assertEquals(200, response.code());
    }

    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<SpanData> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    SpanData spanData = trace.get(0);
    assertEquals(
        "{\"echo\": \"bar\"}",
        spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY));
    assertEquals(
        requestBody, spanData.getAttributes().get(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    server.stop();
  }

  public static final class TestServlet extends HttpServlet {

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException {
      final Map<String, String[]> parameterMap = req.getParameterMap();
      final String echo = parameterMap.get("echo")[0];
      final byte[] responseBody = ("{\"echo\": \"" + echo + "\"}").getBytes(StandardCharsets.UTF_8);
      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setContentLength(responseBody.length);
      try (ServletOutputStream servletOutputStream = resp.getOutputStream()) {
        servletOutputStream.write(responseBody);
      }
    }
  }
}
