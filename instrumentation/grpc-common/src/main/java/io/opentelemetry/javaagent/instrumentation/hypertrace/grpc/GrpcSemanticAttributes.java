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

package io.opentelemetry.javaagent.instrumentation.hypertrace.grpc;

import io.grpc.Metadata;

public class GrpcSemanticAttributes {
  private GrpcSemanticAttributes() {}

  public static final String SCHEME = "scheme";
  public static final String PATH = "path";
  public static final String AUTHORITY = "authority";
  public static final String METHOD = "method";

  /**
   * These metadata headers are added in Http2Headers instrumentation. We use different names than
   * original HTTP2 header names to avoid any collisions with app code.
   *
   * <p>For valid characters in keys read
   * https://grpc.github.io/grpc-java/javadoc/io/grpc/Metadata.Key.html
   */
  private static final String PREFIX = "ht.";

  public static final Metadata.Key<String> SCHEME_METADATA_KEY =
      Metadata.Key.of(PREFIX + SCHEME, Metadata.ASCII_STRING_MARSHALLER);
  public static final Metadata.Key<String> PATH_METADATA_KEY =
      Metadata.Key.of(PREFIX + PATH, Metadata.ASCII_STRING_MARSHALLER);
  public static final Metadata.Key<String> AUTHORITY_METADATA_KEY =
      Metadata.Key.of(PREFIX + AUTHORITY, Metadata.ASCII_STRING_MARSHALLER);
  public static final Metadata.Key<String> METHOD_METADATA_KEY =
      Metadata.Key.of(PREFIX + METHOD, Metadata.ASCII_STRING_MARSHALLER);

  public static String removeHypertracePrefixAndAddColon(String key) {
    if (key.startsWith(PREFIX)) {
      return addColon(key.replaceFirst(PREFIX, ""));
    }
    return key;
  }

  public static String addColon(String key) {
    return ":" + key;
  }
}
