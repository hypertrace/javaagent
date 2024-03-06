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

package org.hypertrace.agent.otel.extensions;
//
// import com.google.auto.service.AutoService;
// import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
// import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.Map;
//
// @AutoService(AutoConfigurationCustomizerProvider.class)
// public final class HypertracePropertySource implements AutoConfigurationCustomizerProvider {
//
//  public Map<String, String> getProperties() {
//    final Map<String, String> configProperties = new HashMap<>();
//    configProperties.put("otel.instrumentation.common.default-enabled", "false");
//
//    configProperties.put("otel.instrumentation.netty.enabled", "true");
//    configProperties.put("otel.instrumentation.servlet.enabled", "true");
//    configProperties.put("otel.instrumentation.vertx-web.enabled", "true");
//    configProperties.put("otel.instrumentation.undertow.enabled", "true");
//    configProperties.put("otel.instrumentation.grpc.enabled", "true");
//
//    configProperties.put("otel.instrumentation.apache-httpasyncclient.enabled", "true");
//    configProperties.put("otel.instrumentation.apache-httpclient.enabled", "true");
//    configProperties.put("otel.instrumentation.okhttp.enabled", "true");
//    configProperties.put("otel.instrumentation.http-url-connection.enabled", "true");
//    configProperties.put("otel.instrumentation.vertx-http-client.enabled", "true");
//
//    configProperties.put("otel.instrumentation.inputstream.enabled", "true");
//    configProperties.put("otel.instrumentation.ht.enabled", "true");
//
//    configProperties.put("otel.instrumentation.methods.enabled", "true");
//    configProperties.put("otel.instrumentation.external-annotations.enabled", "true");
//    configProperties.put(
//        "otel.instrumentation.opentelemetry-extension-annotations.enabled", "true");
//    configProperties.put(
//        "otel.instrumentation.opentelemetry-instrumentation-annotations.enabled", "true");
//    configProperties.put("otel.instrumentation.opentelemetry-api.enabled", "true");
//
//    return Collections.unmodifiableMap(configProperties);
//  }
//
//  @Override
//  public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
//    autoConfigurationCustomizer.addPropertiesSupplier(this::getProperties);
//  }
// }
