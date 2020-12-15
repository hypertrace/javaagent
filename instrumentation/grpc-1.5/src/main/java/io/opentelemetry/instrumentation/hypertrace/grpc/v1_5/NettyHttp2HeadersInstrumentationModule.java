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

package io.opentelemetry.instrumentation.hypertrace.grpc.v1_5;

import static net.bytebuddy.matcher.ElementMatchers.failSafe;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.grpc.Metadata;
import io.netty.handler.codec.http2.Http2Headers;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;

@AutoService(InstrumentationModule.class)
public class NettyHttp2HeadersInstrumentationModule extends InstrumentationModule {

  private static final List<String> INSTRUMENTATION_NAMES = new ArrayList<>();

  static {
    INSTRUMENTATION_NAMES.add(GrpcInstrumentationName.PRIMARY);
    INSTRUMENTATION_NAMES.addAll(Arrays.asList(GrpcInstrumentationName.OTHER));
    INSTRUMENTATION_NAMES.add("grpc-netty-ht");
  }

  public NettyHttp2HeadersInstrumentationModule() {
    super(INSTRUMENTATION_NAMES);
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(new NettyUtilsInstrumentation());
  }

  /**
   * The server side HTTP2 headers are added in tracing gRPC interceptor. The headers are added to
   * metadata in {@link GrpcUtils_convertHeaders_Advice}.
   *
   * <p>The client side HTTP2 headers are added directly to span in {@link
   * Utils_convertClientHeaders_Advice}. TODO However it does not work for the first request
   * https://github.com/hypertrace/javaagent/issues/109#issuecomment-740918018.
   */
  class NettyUtilsInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return failSafe(named("io.grpc.netty.Utils"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
      transformers.put(
          isMethod().and(named("convertClientHeaders")).and(takesArguments(6)),
          Utils_convertClientHeaders_Advice.class.getName());
      transformers.put(
          isMethod().and(named("convertHeaders")).and(takesArguments(1)),
          GrpcUtils_convertHeaders_Advice.class.getName());
      return transformers;
    }
  }

  static class Utils_convertClientHeaders_Advice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.Argument(1) Object scheme,
        @Advice.Argument(2) Object defaultPath,
        @Advice.Argument(3) Object authority,
        @Advice.Argument(4) Object method) {

      Span currentSpan = Java8BytecodeBridge.currentSpan();
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
    }
  }

  /**
   * There are multiple implementations of {@link Http2Headers}. Only some of them support getting
   * authority, path etc. For instance {@code GrpcHttp2ResponseHeaders} throws unsupported exception
   * when accessing authority etc. This header is used client response.
   *
   * @see {@link io.grpc.netty.GrpcHttp2HeadersUtils}
   */
  static class GrpcUtils_convertHeaders_Advice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.Argument(0) Http2Headers http2Headers, @Advice.Return Metadata metadata) {

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
  }
}
