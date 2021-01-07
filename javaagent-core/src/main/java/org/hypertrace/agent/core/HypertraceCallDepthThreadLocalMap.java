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

/**
 * Copy of
 * https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/65f54e450b36bbe2a136fcbf450edd08df1e6c24/javaagent-api/src/main/java/io/opentelemetry/javaagent/instrumentation/api/CallDepthThreadLocalMap.java#L23
 *
 * <p>The copy is needed because in some cases Hypertrace instrumentation wants to use the same
 * class as a key to the map.
 *
 * @see {@link io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap}.
 */
public class HypertraceCallDepthThreadLocalMap {
  private static final ClassValue<HypertraceCallDepthThreadLocalMap.ThreadLocalDepth> TLS =
      new ClassValue<HypertraceCallDepthThreadLocalMap.ThreadLocalDepth>() {
        @Override
        protected HypertraceCallDepthThreadLocalMap.ThreadLocalDepth computeValue(Class<?> type) {
          return new HypertraceCallDepthThreadLocalMap.ThreadLocalDepth();
        }
      };

  public static CallDepth getCallDepth(Class<?> k) {
    return TLS.get(k).get();
  }

  public static int incrementCallDepth(Class<?> k) {
    return TLS.get(k).get().getAndIncrement();
  }

  public static int decrementCallDepth(Class<?> k) {
    return TLS.get(k).get().decrementAndGet();
  }

  public static void reset(Class<?> k) {
    TLS.get(k).get().reset();
  }

  private static final class ThreadLocalDepth extends ThreadLocal<CallDepth> {
    @Override
    protected CallDepth initialValue() {
      return new CallDepth();
    }
  }
}
