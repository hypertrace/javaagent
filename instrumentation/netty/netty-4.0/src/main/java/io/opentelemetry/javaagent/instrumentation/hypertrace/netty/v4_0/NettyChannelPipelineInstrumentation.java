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

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_0.client.HttpClientRequestTracingHandler;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_0.client.HttpClientResponseTracingHandler;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_0.client.HttpClientTracingHandler;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_0.server.HttpServerBlockingRequestHandler;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_0.server.HttpServerRequestTracingHandler;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_0.server.HttpServerResponseTracingHandler;
import io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_0.server.HttpServerTracingHandler;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.HypertraceCallDepthThreadLocalMap;

public class NettyChannelPipelineInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("io.netty.channel.ChannelPipeline");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("io.netty.channel.ChannelPipeline"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(nameStartsWith("add"))
            .and(takesArgument(2, named("io.netty.channel.ChannelHandler"))),
        NettyChannelPipelineInstrumentation.class.getName() + "$ChannelPipelineAddAdvice");
  }

  /**
   * When certain handlers are added to the pipeline, we want to add our corresponding tracing
   * handlers. If those handlers are later removed, we may want to remove our handlers. That is not
   * currently implemented.
   */
  public static class ChannelPipelineAddAdvice {
    @Advice.OnMethodEnter
    public static int trackCallDepth() {
      return HypertraceCallDepthThreadLocalMap.incrementCallDepth(ChannelHandler.class);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void addHandler(
        @Advice.Enter int callDepth,
        @Advice.This ChannelPipeline pipeline,
        @Advice.Argument(2) ChannelHandler handler) {
      if (callDepth > 1) {
        return;
      }
      HypertraceCallDepthThreadLocalMap.reset(ChannelHandler.class);

      try {
        // Server pipeline handlers
        if (handler instanceof HttpServerCodec) {
          // replace OTEL response handler because it closes request span before body (especially
          // chunked) is captured
          pipeline.replace(
              io.opentelemetry.javaagent.instrumentation.netty.v4_0.server.HttpServerTracingHandler
                  .class
                  .getName(),
              HttpServerTracingHandler.class.getName(),
              new HttpServerTracingHandler());

          // add OTEL request handler to start spans
          pipeline.addBefore(
              HttpServerTracingHandler.class.getName(),
              io.opentelemetry.javaagent.instrumentation.netty.v4_0.server
                  .HttpServerRequestTracingHandler.class
                  .getName(),
              new io.opentelemetry.javaagent.instrumentation.netty.v4_0.server
                  .HttpServerRequestTracingHandler());

          pipeline.addLast(
              HttpServerBlockingRequestHandler.class.getName(),
              new HttpServerBlockingRequestHandler());
        } else if (handler instanceof HttpRequestDecoder) {
          pipeline.addLast(
              HttpServerRequestTracingHandler.class.getName(),
              new HttpServerRequestTracingHandler());
        } else if (handler instanceof HttpResponseEncoder) {
          pipeline.replace(
              io.opentelemetry.javaagent.instrumentation.netty.v4_0.server
                  .HttpServerResponseTracingHandler.class
                  .getName(),
              HttpServerResponseTracingHandler.class.getName(),
              new HttpServerResponseTracingHandler());
          pipeline.addLast(
              HttpServerBlockingRequestHandler.class.getName(),
              new HttpServerBlockingRequestHandler());
        } else
        //         Client pipeline handlers
        if (handler instanceof HttpClientCodec) {
          pipeline.replace(
              io.opentelemetry.javaagent.instrumentation.netty.v4_0.client.HttpClientTracingHandler
                  .class
                  .getName(),
              HttpClientTracingHandler.class.getName(),
              new HttpClientTracingHandler());

          // add OTEL request handler to start spans
          pipeline.addAfter(
              HttpClientTracingHandler.class.getName(),
              io.opentelemetry.javaagent.instrumentation.netty.v4_0.client
                  .HttpClientRequestTracingHandler.class
                  .getName(),
              new io.opentelemetry.javaagent.instrumentation.netty.v4_0.client
                  .HttpClientRequestTracingHandler());
        } else if (handler instanceof HttpRequestEncoder) {
          pipeline.addLast(
              HttpClientRequestTracingHandler.class.getName(),
              new HttpClientRequestTracingHandler());
        } else if (handler instanceof HttpResponseDecoder) {
          pipeline.replace(
              io.opentelemetry.javaagent.instrumentation.netty.v4_0.client
                  .HttpClientResponseTracingHandler.class
                  .getName(),
              HttpClientResponseTracingHandler.class.getName(),
              new HttpClientResponseTracingHandler());
        }
      } catch (IllegalArgumentException e) {
        // Prevented adding duplicate handlers.
      }
    }
  }
}
