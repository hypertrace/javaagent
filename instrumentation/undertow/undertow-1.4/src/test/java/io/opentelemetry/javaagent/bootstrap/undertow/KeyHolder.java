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

import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Undertow's {@code io.undertow.server.HttpServerExchange} uses {@code
 * io.undertow.util.AttachmentKey} as a key for storing arbitrary data. It uses {@link
 * IdentityHashMap} and thus all keys are compared for equality by object reference. This means that
 * we cannot hold an instance of {@code io.undertow.util.AttachmentKey} in a static field of the
 * corresponding Tracer, as we usually do. Tracers are loaded into user's classloaders and thus it
 * is totally possible to have several instances of tracers. Each of those instances will have a
 * separate value in a static field and {@code io.undertow.server.HttpServerExchange} will treat
 * them as different keys then.
 *
 * <p>That is why this class exists and resides in a separate package. This package is treated in a
 * special way and is always loaded by bootstrap classloader. This makes sure that this class is
 * available to all tracers from every classloader.
 *
 * <p>But at the same time, being loaded by bootstrap classloader, this class itself cannot initiate
 * the loading of {@code io.undertow.util.AttachmentKey} class. Class has to be loaded by <i>any</i>
 * classloader that has it, e.g. by the classloader of a Tracer that uses this key holder. After
 * that, <i>all</i> Tracers, loaded by all classloaders, will be able to use exactly the same sole
 * instance of the key.
 */
// TODO allow instrumentation to have their own classes that should go to bootstrap classloader
public final class KeyHolder {
  public static final ConcurrentMap<Class<?>, Object> contextKeys = new ConcurrentHashMap<>();

  private KeyHolder() {}
}
