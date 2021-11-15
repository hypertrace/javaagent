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
 * {@link FunctionalInterface} modeled off of {@link java.util.function.BiFunction} so that this
 * module can accept generic method references as opposed to depending directly on the filter API.
 *
 * @param <P1> The type of the first parameter
 * @param <P2> The type of the second parameter
 * @param <P3> The type of the third parameter
 * @param <R> The return type
 */
@FunctionalInterface
public interface TriFunction<P1, P2, P3, R> {

  R apply(P1 p1, P2 p2, P3 p3);
}
