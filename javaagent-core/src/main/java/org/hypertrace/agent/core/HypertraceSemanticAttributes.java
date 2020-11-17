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

package org.hypertrace.agent.core;

import io.opentelemetry.api.common.AttributeKey;


public class HypertraceSemanticAttributes {
  private HypertraceSemanticAttributes() {}

  public static AttributeKey<String> httpRequestHeader(String header) {
    return AttributeKey.stringKey("http.request.header." + header);
  }

  public static AttributeKey<String> httpResponseHeader(String header) {
    return AttributeKey.stringKey("http.response.header." + header);
  }

  public static final AttributeKey<String> HTTP_REQUEST_BODY = AttributeKey.stringKey("http.request.body");
  public static final AttributeKey<String> HTTP_RESPONSE_BODY = AttributeKey.stringKey("http.response.body");

  public static final AttributeKey<String> HTTP_REQUEST_SESSION_ID =
      AttributeKey.stringKey("http.request.session_id");

  public static final AttributeKey<String> RPC_REQUEST_BODY = AttributeKey.stringKey("rpc.request.body");
  public static final AttributeKey<String> RPC_RESPONSE_BODY = AttributeKey.stringKey("rpc.response.body");

  public static final AttributeKey<String> rpcRequestMetadata(String key) {
    return AttributeKey.stringKey("rpc.request.metadata." + key);
  }

  public static final AttributeKey<String> rpcResponseMetadata(String key) {
    return AttributeKey.stringKey("rpc.response.metadata." + key);
  }
}
