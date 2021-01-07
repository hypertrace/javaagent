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

package org.hypertrace.agent.smoketest;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.ResponseBody;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public abstract class AbstractSmokeTest {
  private static final Logger log = LoggerFactory.getLogger(OpenTelemetryStorage.class);
  private static final String OTEL_COLLECTOR_IMAGE = "otel/opentelemetry-collector:latest";
  private static final String MOCK_BACKEND_IMAGE =
      "open-telemetry-docker-dev.bintray.io/java/smoke-fake-backend:latest";
  private static final String NETWORK_ALIAS_OTEL_COLLECTOR = "collector";
  private static final String NETWORK_ALIAS_OTEL_MOCK_STORAGE = "storage";
  private static final String OTEL_EXPORTER_ENDPOINT =
      String.format("http://%s:9411/api/v2/spans", NETWORK_ALIAS_OTEL_COLLECTOR);

  public static final String OTEL_LIBRARY_VERSION_ATTRIBUTE = "otel.library.version";
  public static final String agentPath = getPropertyOrEnv("smoketest.javaagent.path");

  private static final Network network = Network.newNetwork();
  private static OpenTelemetryCollector collector;
  private static OpenTelemetryStorage openTelemetryStorage;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int DEFAULT_TIMEOUT_SECONDS = 30;
  protected OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .followRedirects(true)
          .build();

  @BeforeAll
  public static void beforeAll() {
    openTelemetryStorage =
        new OpenTelemetryStorage(MOCK_BACKEND_IMAGE)
            .withNetwork(network)
            .withNetworkAliases(NETWORK_ALIAS_OTEL_MOCK_STORAGE)
            .withLogConsumer(new Slf4jLogConsumer(log));
    openTelemetryStorage.start();

    collector =
        new OpenTelemetryCollector(OTEL_COLLECTOR_IMAGE)
            .withNetwork(network)
            .withNetworkAliases(NETWORK_ALIAS_OTEL_COLLECTOR)
            .withLogConsumer(new Slf4jLogConsumer(log))
            .dependsOn(openTelemetryStorage)
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("/otelcol-config.yaml"),
                "/etc/otelcol-config.yaml")
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withCommand("--config /etc/otelcol-config.yaml");
    collector.start();
  }

  @AfterAll
  public static void afterAll() {
    collector.close();
    openTelemetryStorage.close();
    network.close();
  }

  @AfterEach
  void cleanData() throws IOException {
    client
        .newCall(
            new Request.Builder()
                .url(
                    String.format(
                        "http://localhost:%d/clear-requests", openTelemetryStorage.getPort()))
                .build())
        .execute()
        .close();
  }

  protected abstract String getTargetImage(int jdk);

  GenericContainer createAppUnderTest(int jdk) {
    if (agentPath == null || agentPath.isEmpty()) {
      throw new IllegalStateException(
          "agentPath is not set, configure it via env var SMOKETEST_JAVAAGENT_PATH");
    }
    log.info("Agent path {}", agentPath);
    return new GenericContainer<>(DockerImageName.parse(getTargetImage(jdk)))
        .withExposedPorts(8080)
        .withNetwork(network)
        .withLogConsumer(new Slf4jLogConsumer(log))
        .withCopyFileToContainer(MountableFile.forHostPath(agentPath), "/javaagent.jar")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("/ht-config.yaml"), "/etc/ht-config.yaml")
        .withEnv("JAVA_TOOL_OPTIONS", "-javaagent:/javaagent.jar")
        .withEnv("HT_CONFIG_FILE", "/etc/ht-config.yaml")
        .withEnv("OTEL_BSP_MAX_EXPORT_BATCH", "1")
        .withEnv("OTEL_BSP_SCHEDULE_DELAY", "10")
        .withEnv("HT_REPORTING_ENDPOINT", OTEL_EXPORTER_ENDPOINT);
  }

  protected static int countSpansByName(
      Collection<ExportTraceServiceRequest> traces, String spanName) {
    return (int) getSpanStream(traces).filter(it -> it.getName().equals(spanName)).count();
  }

  protected static Stream<Span> getSpanStream(Collection<ExportTraceServiceRequest> traceRequest) {
    return traceRequest.stream()
        .flatMap(request -> request.getResourceSpansList().stream())
        .flatMap(resourceSpans -> resourceSpans.getInstrumentationLibrarySpansList().stream())
        .flatMap(librarySpans -> librarySpans.getSpansList().stream());
  }

  protected Collection<ExportTraceServiceRequest> waitForTraces() throws IOException {
    String content = waitForContent();

    return StreamSupport.stream(OBJECT_MAPPER.readTree(content).spliterator(), false)
        .map(
            it -> {
              ExportTraceServiceRequest.Builder builder = ExportTraceServiceRequest.newBuilder();
              try {
                JsonFormat.parser().merge(OBJECT_MAPPER.writeValueAsString(it), builder);
              } catch (InvalidProtocolBufferException | JsonProcessingException e) {
                e.printStackTrace();
              }
              return builder.build();
            })
        .collect(Collectors.toList());
  }

  private String waitForContent() throws IOException {
    Request request =
        new Builder()
            .url(String.format("http://localhost:%d/get-requests", openTelemetryStorage.getPort()))
            .build();
    // TODO do not use local vars in lambda
    final AtomicLong previousSize = new AtomicLong();
    Awaitility.await()
        .until(
            () -> {
              try (ResponseBody body = client.newCall(request).execute().body()) {
                String content = body.string();
                if (content.length() > "[]".length() && content.length() == previousSize.get()) {
                  return true;
                }
                previousSize.set(content.length());
                log.debug("Current content size {}", previousSize.get());
              }
              return false;
            });
    return client.newCall(request).execute().body().string();
  }

  public static String getPropertyOrEnv(String propName) {
    String property = System.getProperty(propName);
    if (property != null && !property.isEmpty()) {
      return property;
    }
    return System.getenv(propName.toUpperCase().replaceAll("\\.", "_"));
  }
}
