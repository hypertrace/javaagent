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

package org.hypertrace.agent.core.instrumentation;

import io.opentelemetry.api.common.AttributeKey;

public class HypertraceSemanticAttributes {
  private HypertraceSemanticAttributes() {}

  /**
   * Span name used for span that carries additional data (e.g. attributes) that should belong to
   * its parent. The parent span cannot carry additional data because it has been already finished.
   * This usually happens when capturing response body in RPC client instrumentations - the body is
   * read after the client span is finished.
   */
  public static final String ADDITIONAL_DATA_SPAN_NAME = "additional-data";

  public static AttributeKey<String> httpRequestHeader(String header) {
    return AttributeKey.stringKey("http.request.header." + header.toLowerCase());
  }

  public static AttributeKey<String> httpResponseHeader(String header) {
    return AttributeKey.stringKey("http.response.header." + header.toLowerCase());
  }

  public static AttributeKey<Long> httpResponseHeaderLong(String header) {
    return AttributeKey.longKey("http.response.header." + header.toLowerCase());
  }

  public static final AttributeKey<String> HTTP_REQUEST_BODY =
      AttributeKey.stringKey("http.request.body");
  public static final AttributeKey<String> HTTP_RESPONSE_BODY =
      AttributeKey.stringKey("http.response.body");

  public static final AttributeKey<String> HTTP_REQUEST_SESSION_ID =
      AttributeKey.stringKey("http.request.session_id");
  public static final AttributeKey<String> HTTP_RESPONSE_CONTENT_TYPE =
      AttributeKey.stringKey("http.response.header.content-type");
  public static final AttributeKey<Long> HTTP_RESPONSE_CONTENT_LENGTH =
      AttributeKey.longKey("http.response.header.content-length");

  public static final AttributeKey<String> HTTP_REQUEST_HEADER_CONTENT_TYPE =
      AttributeKey.stringKey("http.request.header.content-type");

  public static final AttributeKey<String> HTTP_RESPONSE_HEADER_CONTENT_TYPE =
      AttributeKey.stringKey("http.response.header.content-type");

  public static final AttributeKey<String> HTTP_RESPONSE_HEADER_CONTENT_ENCODING =
      AttributeKey.stringKey("http.response.header.content-encoding");

  public static final AttributeKey<String> RPC_REQUEST_BODY =
      AttributeKey.stringKey("rpc.request.body");
  public static final AttributeKey<String> RPC_RESPONSE_BODY =
      AttributeKey.stringKey("rpc.response.body");

  public static AttributeKey<String> rpcRequestMetadata(String key) {
    return AttributeKey.stringKey("rpc.request.metadata." + key.toLowerCase());
  }

  public static AttributeKey<String> rpcResponseMetadata(String key) {
    return AttributeKey.stringKey("rpc.response.metadata." + key.toLowerCase());
  }
}
