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

package io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.netty.shaded.v1_9;

import static net.bytebuddy.matcher.ElementMatchers.failSafe;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Headers;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.netty.shaded.v1_9.utils.NettyUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public final class ShadedNettyHttp2HeadersInstrumentationModule extends InstrumentationModule {

  public ShadedNettyHttp2HeadersInstrumentationModule() {
    super("grpc-netty", "ht", "grpc-netty-ht");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new ShadedNettyUtilsInstrumentation());
  }

  static final class ShadedNettyUtilsInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return failSafe(named("io.grpc.netty.shaded.io.grpc.netty.Utils"));
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
      NettyUtils.handleConvertClientHeaders(scheme, defaultPath, authority, method, currentSpan);
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

      NettyUtils.handleConvertHeaders(http2Headers, metadata);
    }
  }
}
