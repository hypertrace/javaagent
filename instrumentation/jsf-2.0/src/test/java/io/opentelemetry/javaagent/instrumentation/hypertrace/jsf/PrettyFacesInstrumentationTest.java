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

package io.opentelemetry.javaagent.instrumentation.hypertrace.jsf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class PrettyFacesInstrumentationTest extends AbstractInstrumenterTest {

  private Server jettyServer;

  @BeforeEach
  void startServer() throws Exception {
    final ArrayList<String> configurationClasses = new ArrayList<>();
    Collections.addAll(configurationClasses, WebAppContext.getDefaultConfigurationClasses());
    configurationClasses.add(AnnotationConfiguration.class.getName());

    WebAppContext webAppContext = new WebAppContext();
    final String contextPath = "/jetty-context";
    webAppContext.setContextPath(contextPath);
    webAppContext.setConfigurationClasses(configurationClasses);

    // set up test application
    webAppContext.setBaseResource(Resource.newSystemResource("test-app-2"));

    webAppContext.getMetaData().getWebInfClassesDirs().add(Resource.newClassPathResource("/"));

    jettyServer = new Server(8080);
    //    for ( Connector connector : jettyServer.getConnectors()) {
    //      connector
    //    }

    jettyServer.setHandler(webAppContext);
    jettyServer.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    jettyServer.stop();
    jettyServer.destroy();
  }

  @Test
  void foo() throws IOException, InterruptedException, TimeoutException {
    final HttpUrl url =
        new Builder()
            .host("localhost")
            .scheme("http")
            .port(8080)
            .addPathSegment("jetty-context")
            .addPathSegment("hello.xhtml")
            .build();

    try (Response response =
            httpClient
                .newCall(
                    new Request.Builder()
                        .url(url)
                        .post(
                            new FormBody.Builder()
                                .add("app-form", "app-form")
                                .add("app-form:name", "test")
                                .add("app-form:submit", "Say hello")
                                .add("app-form_SUBMIT", "1") // MyFaces
                                .build())
                        .build())
                .execute();
        final ResponseBody responseBody = response.body()) {
      assertEquals(200, response.code());
      assertNotNull(responseBody);
      assertTrue(responseBody.string().contains("Say hello"));
    }

    List<List<SpanData>> traces = TEST_WRITER.getTraces();
    TEST_WRITER.waitForTraces(1);
    Assertions.assertEquals(1, traces.size());
    List<SpanData> trace = traces.get(0);
    Assertions.assertEquals(1, trace.size());
    SpanData spanData = trace.get(0);
    assertNotNull(spanData);
  }
}
