package io.opentelemetry.javaagent.instrumentation.hypertrace.jsf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import javax.faces.view.facelets.FaceletCache;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
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
  void foo() throws IOException {
    final HttpUrl url = new Builder().host("localhost").scheme("http").port(8080)

        .addPathSegment("jetty-context").addPathSegment("hello.xhtml")
        .build();

    final FormBody formBody = new FormBody.Builder()
        .add("app-form", "app-form")
        .add("app-form:name", "test")
        .add("app-form:submit", "Say hello")
        .add("app-form_SUBMIT", "1") // MyFaces
        .build();
    try (Response response = httpClient
        .newCall(new Request.Builder()
            .url(url).post(
                RequestBody.create("foo", MediaType.get("application/x-www-form-urlencoded")))
            .build())
        .execute()) {
      assertEquals(200, response.code());
    }
  }

}
