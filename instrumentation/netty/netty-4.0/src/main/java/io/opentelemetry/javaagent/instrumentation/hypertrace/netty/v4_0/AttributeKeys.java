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

package io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_0;

import io.netty.util.AttributeKey;
import io.opentelemetry.javaagent.instrumentation.api.WeakMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.hypertrace.agent.core.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;

/**
 * Copied from
 * https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/deda1af9c0e420b882164e5f8240b51678cd646f/instrumentation/netty/netty-4.0/javaagent/src/main/java/io/opentelemetry/javaagent/instrumentation/netty/v4_0/AttributeKeys.java#L14
 */
public class AttributeKeys {
  private static final WeakMap<ClassLoader, ConcurrentMap<String, AttributeKey<?>>> map =
      WeakMap.Implementation.DEFAULT.get();
  private static final WeakMap.ValueSupplier<ClassLoader, ConcurrentMap<String, AttributeKey<?>>>
      mapSupplier =
          new WeakMap.ValueSupplier<ClassLoader, ConcurrentMap<String, AttributeKey<?>>>() {
            @Override
            public ConcurrentMap<String, AttributeKey<?>> get(ClassLoader ignore) {
              return new ConcurrentHashMap<>();
            }
          };

  public static final AttributeKey<BoundedByteArrayOutputStream> RESPONSE_BODY_BUFFER =
      attributeKey(HypertraceSemanticAttributes.HTTP_RESPONSE_BODY.getKey());

  public static final AttributeKey<BoundedByteArrayOutputStream> REQUEST_BODY_BUFFER =
      attributeKey(HypertraceSemanticAttributes.HTTP_REQUEST_BODY.getKey());

  public static final AttributeKey<Map<String, String>> REQUEST_HEADERS =
      attributeKey(AttributeKeys.class.getName() + ".request-headers");

  /**
   * Generate an attribute key or reuse the one existing in the global app map. This implementation
   * creates attributes only once even if the current class is loaded by several class loaders and
   * prevents an issue with Apache Atlas project were this class loaded by multiple class loaders,
   * while the Attribute class is loaded by a third class loader and used internally for the
   * cassandra driver.
   */
  private static <T> AttributeKey<T> attributeKey(String key) {
    ConcurrentMap<String, AttributeKey<?>> classLoaderMap =
        map.computeIfAbsent(AttributeKey.class.getClassLoader(), mapSupplier);
    if (classLoaderMap.containsKey(key)) {
      return (AttributeKey<T>) classLoaderMap.get(key);
    }

    AttributeKey<T> value = new AttributeKey<>(key);
    classLoaderMap.put(key, value);
    return value;
  }
}
