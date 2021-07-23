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

package io.opentelemetry.javaagent.instrumentation.hypertrace.undertow.v1_4;

import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.hypertrace.undertow.v1_4.utils.Utils;
import io.undertow.server.HttpServerExchange;
import java.nio.ByteBuffer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.instrumentation.HypertraceCallDepthThreadLocalMap;
import org.hypertrace.agent.core.instrumentation.SpanAndBuffer;
import org.xnio.channels.StreamSourceChannel;

/** Instrumentation for {@link StreamSourceChannel} implementations */
public final class StreamSourceChannelInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return AgentElementMatchers.safeHasSuperType(named("org.xnio.channels.StreamSourceChannel"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("read")
            .and(takesArguments(1))
            .and(takesArgument(0, ByteBuffer.class))
            .and(returns(int.class))
            .and(isPublic()),
        StreamSourceChannelInstrumentation.class.getName() + "$Read_advice");
  }

  /**
   * Decorates the {@link StreamSourceChannel#read(ByteBuffer)} implementations with logic to
   * capture data read into the request body {@link ByteBuffer} and report it. This instrumentation
   * short-circuits if:
   *
   * <ul>
   *   <li>We're in a nested {@link StreamSourceChannel#read(ByteBuffer)} call
   *   <li>A {@link Throwable} was thrown in the context of the {@link
   *       StreamSourceChannel#read(ByteBuffer)}, causing the method to exit
   *   <li>The instrumented {@link StreamSourceChannel} was never put in the {@link
   *       InstrumentationContext} by {@link
   *       UndertowHttpServerExchangeInstrumentation.GetRequestChannel_advice#exit(HttpServerExchange,
   *       StreamSourceChannel)}
   * </ul>
   */
  static final class Read_advice {

    @Advice.OnMethodEnter
    public static void trackCallDepth() {
      HypertraceCallDepthThreadLocalMap.incrementCallDepth(StreamSourceChannel.class);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Return final int numBytesRead,
        @Advice.This final StreamSourceChannel streamSourceChannel,
        @Advice.Thrown final Throwable thrown,
        @Advice.Argument(0) final ByteBuffer byteBuffer) {
      if (HypertraceCallDepthThreadLocalMap.decrementCallDepth(StreamSourceChannel.class) > 0
          || thrown != null) {
        return;
      }
      final ContextStore<StreamSourceChannel, SpanAndBuffer> contextStore =
          InstrumentationContext.get(StreamSourceChannel.class, SpanAndBuffer.class);
      final SpanAndBuffer spanAndBuffer = contextStore.get(streamSourceChannel);
      if (spanAndBuffer != null) {
        Utils.handleRead(byteBuffer.asReadOnlyBuffer(), numBytesRead, spanAndBuffer);
      }
    }
  }
}
