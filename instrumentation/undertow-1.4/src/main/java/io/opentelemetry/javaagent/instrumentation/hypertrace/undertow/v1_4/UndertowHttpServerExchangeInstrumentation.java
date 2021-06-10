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

import static net.bytebuddy.matcher.ElementMatchers.failSafe;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.hypertrace.undertow.v1_4.utils.Utils;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import io.undertow.server.HttpServerExchange;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import org.hypertrace.agent.core.instrumentation.SpanAndBuffer;
import org.xnio.channels.StreamSourceChannel;

/** Instrumentation for {@link HttpServerExchange} to capture request bodies */
public final class UndertowHttpServerExchangeInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return failSafe(named("io.undertow.server.HttpServerExchange"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    final Map<Junction<MethodDescription>, String> matchers = new HashMap<>();
    matchers.put(
        named("getRequestChannel")
            .and(takesArguments(0))
            .and(returns(named("org.xnio.channels.StreamSourceChannel")))
            .and(isPublic()),
        GetRequestChannel_advice.class.getName());
    return Collections.unmodifiableMap(matchers);
  }

  /**
   * Decorates {@link HttpServerExchange#getRequestChannel()} with instrumentation to store a {@link
   * SpanAndBuffer} in the {@link InstrumentationContext}
   */
  static final class GetRequestChannel_advice {

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This final HttpServerExchange thizz,
        @Advice.Return final StreamSourceChannel returnedChannel) {
      final ContextStore<StreamSourceChannel, SpanAndBuffer> contextStore =
          InstrumentationContext.get(StreamSourceChannel.class, SpanAndBuffer.class);
      if (contextStore.get(returnedChannel) != null) {
        // HttpServerExchange.getRequestChannel only creates a new channel the first time it is
        // invoked.on subsequent invocations, we do not want to create a new buffer and put it in
        // the context, as that would reset the state and could potential result in lost request
        // bodies
        return;
      }
      Utils.createAndStoreBufferForSpan(thizz, returnedChannel, contextStore);
    }
  }
}
