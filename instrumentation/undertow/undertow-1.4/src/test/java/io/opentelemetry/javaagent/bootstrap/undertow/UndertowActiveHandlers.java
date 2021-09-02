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

package io.opentelemetry.javaagent.bootstrap.undertow;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import java.util.concurrent.atomic.AtomicInteger;

/** Helper container for keeping track of request processing state in undertow. */
public final class UndertowActiveHandlers {
  private static final ContextKey<AtomicInteger> CONTEXT_KEY =
      ContextKey.named("opentelemetry-undertow-active-handlers");

  private UndertowActiveHandlers() {}

  /**
   * Attach to context.
   *
   * @param context server context
   * @param initialValue initial value for counter
   * @return new context
   */
  public static Context init(Context context, int initialValue) {
    return context.with(CONTEXT_KEY, new AtomicInteger(initialValue));
  }

  /**
   * Increment counter.
   *
   * @param context server context
   */
  public static void increment(Context context) {
    context.get(CONTEXT_KEY).incrementAndGet();
  }

  /**
   * Decrement counter.
   *
   * @param context server context
   * @return value of counter after decrementing it
   */
  public static int decrementAndGet(Context context) {
    return context.get(CONTEXT_KEY).decrementAndGet();
  }
}
