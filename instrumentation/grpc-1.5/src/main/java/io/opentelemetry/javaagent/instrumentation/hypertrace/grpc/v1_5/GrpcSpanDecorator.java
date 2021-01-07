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

package io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.v1_5;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcSpanDecorator {

  private GrpcSpanDecorator() {}

  private static final Logger log = LoggerFactory.getLogger(GrpcSpanDecorator.class);
  private static final JsonFormat.Printer PRINTER = JsonFormat.printer();

  public static void addMessageAttribute(Object message, Span span, AttributeKey<String> key) {
    if (message instanceof Message) {
      Message mb = (Message) message;
      try {
        String requestBody = PRINTER.print(mb);
        span.setAttribute(key, requestBody);
      } catch (InvalidProtocolBufferException e) {
        log.error("Failed to decode message to JSON", e);
      }
    }
  }

  public static void addMetadataAttributes(
      Metadata metadata, Span span, Function<String, AttributeKey<String>> keySupplier) {
    for (String key : metadata.keys()) {
      if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
        // do not add binary metadata
        continue;
      }
      Key<String> stringKey = Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
      Iterable<String> stringValues = metadata.getAll(stringKey);
      for (String stringValue : stringValues) {
        key = GrpcSemanticAttributes.removeHypertracePrefixAndAddColon(key);
        span.setAttribute(keySupplier.apply(key), stringValue);
      }
    }
  }

  public static void addMetadataAttributes(Map<String, String> metadata, Span span) {
    for (Map.Entry<String, String> entry : metadata.entrySet()) {
      span.setAttribute(entry.getKey(), entry.getValue());
    }
  }

  public static Map<String, String> metadataToMap(Metadata metadata) {
    Map<String, String> mapHeaders = new LinkedHashMap<>(metadata.keys().size());
    for (String key : metadata.keys()) {
      if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
        continue;
      }
      Key<String> stringKey = Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
      Iterable<String> stringValues = metadata.getAll(stringKey);
      for (String stringValue : stringValues) {
        key = GrpcSemanticAttributes.removeHypertracePrefixAndAddColon(key);
        mapHeaders.put(HypertraceSemanticAttributes.rpcRequestMetadata(key).getKey(), stringValue);
      }
    }
    return mapHeaders;
  }
}
