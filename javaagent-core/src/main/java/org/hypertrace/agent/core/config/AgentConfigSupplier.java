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

package org.hypertrace.agent.core.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.util.function.Supplier;
import org.hypertrace.agent.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memoizing supplier inspired by Guava's <a
 * href="https://github.com/google/guava/blob/master/guava/src/com/google/common/base/Suppliers.java#L150">NonSerializableMemoizingSupplier</a>.
 * Decorates a {@link Supplier} of {@link Config.AgentConfig} with memoizing, logging, and
 * error-handling logic for a consistent once-per Java process Config initialization.
 *
 * <p>Thread-safe
 */
public final class AgentConfigSupplier implements Supplier<Config.AgentConfig> {

  private static final Logger log = LoggerFactory.getLogger(AgentConfigSupplier.class);

  private final Supplier<Config.AgentConfig> supplier;
  private volatile boolean initialized;
  /**
   * Although not explicitly volatile, this field is effectively volatile because updates to this
   * field are always followed by updates to the volatile field {@link #initialized}. In {@link
   * AgentConfigSupplier#get()} due to the semantics of how the volatile keyword is implemented.
   * Namely, anything (not just volatile fields) that was visible to thread A when it writes to
   * volatile field {@link #initialized} becomes visible to thread B when it reads {@link
   * #initialized}.
   */
  private Config.AgentConfig value;

  /**
   * @param supplier inner supplier of {@link Config.AgentConfig} to decorate with config loading
   *     logic
   */
  public AgentConfigSupplier(final Supplier<Config.AgentConfig> supplier) {
    this.supplier = supplier;
  }

  /**
   * Delegates to an inner composed {@link Supplier} the first time this method is invoked by any
   * thread, and caches the value in the effectively volatile field {@link #value}. On subsequent
   * invocations, the {@link #value} will be returned without invocation of the {@link #supplier},
   * unless {@link #reset()} has been invoked since the first invocation of this method.
   *
   * @return the {@link Config.AgentConfig} singleton.
   */
  @Override
  public Config.AgentConfig get() {
    if (!initialized) {
      synchronized (this) {
        if (!initialized) {
          final Config.AgentConfig result = supplier.get();
          value = result;
          initialized = true;
          try {
            log.info(
                "Config loaded: {}",
                JsonFormat.printer().omittingInsignificantWhitespace().print(result));
          } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("Error serializing protobuf config for writing to log", e);
          }
          return result;
        }
      }
    }
    return value;
  }

  /**
   * Resets the memoizing aspect of this {@link Supplier}, forcing the invocation of the inner
   * {@link #supplier} once again. Because this method exists, we cannot dereference the {@link
   * #supplier} field to make it eligible for GC as done by the Gauva {@code
   * NonSerializableMemoizingSupplier}.
   *
   * <p>There's no "double-checked locking" optimization to make here. In the case of resetting, we
   * might consider checking the volatile field {@link #initialized} prior to acquiring the lock on
   * the instance and short-circuiting if {@link #initialized} is true. However, this API is only
   * visible for testing purposes and we only expect it to be called after the {@link
   * Config.AgentConfig} is loaded. So we simply want to make sure this class is thread-safe by
   * making sure updates to the {@link #initialized} and {@link #value} field are atomic
   */
  @VisibleForTesting
  void reset() {
    synchronized (this) {
      if (initialized) {
        value = null;
        initialized = false;
      }
    }
  }
}
