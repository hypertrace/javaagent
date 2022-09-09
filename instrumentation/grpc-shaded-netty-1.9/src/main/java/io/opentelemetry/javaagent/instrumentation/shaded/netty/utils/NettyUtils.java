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

package io.opentelemetry.javaagent.instrumentation.shaded.netty.utils;

import io.grpc.Metadata;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Headers;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.GrpcSemanticAttributes;
import io.opentelemetry.javaagent.instrumentation.hypertrace.utils.SpanUtils;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;

public class NettyUtils {

  public static void handleConvertHeaders(Http2Headers http2Headers, Metadata metadata) {
    if (http2Headers.authority() != null) {
      metadata.put(
          GrpcSemanticAttributes.AUTHORITY_METADATA_KEY, http2Headers.authority().toString());
    }
    if (http2Headers.path() != null) {
      metadata.put(GrpcSemanticAttributes.PATH_METADATA_KEY, http2Headers.path().toString());
    }
    if (http2Headers.method() != null) {
      metadata.put(GrpcSemanticAttributes.METHOD_METADATA_KEY, http2Headers.method().toString());
    }
    if (http2Headers.scheme() != null) {
      metadata.put(GrpcSemanticAttributes.SCHEME_METADATA_KEY, http2Headers.scheme().toString());
    }
  }

  public static void handleConvertClientHeaders(
      Object scheme, Object defaultPath, Object authority, Object method, Span currentSpan) {
    if (scheme != null) {
      currentSpan.setAttribute(
          HypertraceSemanticAttributes.rpcRequestMetadata(
              GrpcSemanticAttributes.addColon(GrpcSemanticAttributes.SCHEME)),
          scheme.toString());
    }
    if (defaultPath != null) {
      currentSpan.setAttribute(
          HypertraceSemanticAttributes.rpcRequestMetadata(
              GrpcSemanticAttributes.addColon(GrpcSemanticAttributes.PATH)),
          defaultPath.toString());
    }
    if (authority != null) {
      currentSpan.setAttribute(
          HypertraceSemanticAttributes.rpcRequestMetadata(
              GrpcSemanticAttributes.addColon(GrpcSemanticAttributes.AUTHORITY)),
          authority.toString());
    }
    if (method != null) {
      currentSpan.setAttribute(
          HypertraceSemanticAttributes.rpcRequestMetadata(
              GrpcSemanticAttributes.addColon(GrpcSemanticAttributes.METHOD)),
          method.toString());
    }
    SpanUtils.setSpanAttributes(currentSpan);
  }
}
