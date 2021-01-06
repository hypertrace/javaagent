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

package io.opentelemetry.javaagent.instrumentation.hypertrace.vertx.netty;

import static io.opentelemetry.javaagent.tooling.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.hypertrace.vertx.netty.server.HttpServerRequestTracingHandler;
import io.opentelemetry.javaagent.instrumentation.hypertrace.vertx.netty.server.HttpServerResponseTracingHandler;
import io.opentelemetry.javaagent.instrumentation.hypertrace.vertx.netty.server.NettyHttpServerTracingHandler;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

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
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        isMethod()
            .and(nameStartsWith("add"))
            .and(takesArgument(2, named("io.netty.channel.ChannelHandler"))),
        NettyChannelPipelineInstrumentation.class.getName() + "$ChannelPipelineAddAdvice");
    return transformers;
  }

  /**
   * When certain handlers are added to the pipeline, we want to add our corresponding tracing
   * handlers. If those handlers are later removed, we may want to remove our handlers. That is not
   * currently implemented.
   */
  public static class ChannelPipelineAddAdvice {
    @Advice.OnMethodEnter
    public static int trackCallDepth() {
      return CallDepthThreadLocalMap.incrementCallDepth(ChannelHandler.class);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void addHandler(
        @Advice.Enter int callDepth,
        @Advice.This ChannelPipeline pipeline,
        @Advice.Argument(2) ChannelHandler handler) {
      if (callDepth > 0) {
        return;
      }
      CallDepthThreadLocalMap.reset(ChannelHandler.class);

      try {
        //        if (handler instanceof
        // io.opentelemetry.javaagent.instrumentation.netty.v4_0.server.HttpServerResponseTracingHandler) {
        //          System.out.println("Removing\n\n");
        //
        // pipeline.remove(io.opentelemetry.javaagent.instrumentation.netty.v4_0.server.HttpServerResponseTracingHandler.class.getName());
        //        }

        // Server pipeline handlers
        if (handler instanceof HttpServerCodec) {
          pipeline.addLast(
              NettyHttpServerTracingHandler.class.getName(), new NettyHttpServerTracingHandler());

        } else if (handler instanceof HttpRequestDecoder) {
          System.out.println("\n\nAdding request handler");
          pipeline.addLast(
              HttpServerRequestTracingHandler.class.getName(),
              new HttpServerRequestTracingHandler());
        } else if (handler instanceof HttpResponseEncoder) {
          System.out.println("\n\nAdding response handler");
          // replace OTEL response handler because it closes request span before body (especially
          // chunked) is captured
          pipeline.replace(
              io.opentelemetry.javaagent.instrumentation.netty.v4_0.server
                  .HttpServerResponseTracingHandler.class
                  .getName(),
              HttpServerResponseTracingHandler.class.getName(),
              new HttpServerResponseTracingHandler());
        }
        //        else
        // Client pipeline handlers
        //          if (handler instanceof HttpClientCodec) {
        //            pipeline.addLast(
        //                HttpClientTracingHandler.class.getName(), new HttpClientTracingHandler());
        //          } else if (handler instanceof HttpRequestEncoder) {
        //            pipeline.addLast(
        //                HttpClientRequestTracingHandler.class.getName(),
        //                new HttpClientRequestTracingHandler());
        //          } else if (handler instanceof HttpResponseDecoder) {
        //            pipeline.addLast(
        //                HttpClientResponseTracingHandler.class.getName(),
        //                new HttpClientResponseTracingHandler());
        //          }
      } catch (IllegalArgumentException e) {
        // Prevented adding duplicate handlers.
      }

      System.out.println("\nPipeline names");
      for (String name : pipeline.names()) {
        ChannelHandler channelHandler = pipeline.get(name);
        if (channelHandler == null) {
          System.out.printf("channelHandler is null: %s\n", name);
        } else {
          System.out.printf("%s:%s\n", name, channelHandler.getClass().getCanonicalName());
        }
      }
    }
  }
}
