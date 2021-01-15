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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;

public class BufferedReaderInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(named("java.io.BufferedReader"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<Junction<MethodDescription>, String> transformers = new HashMap<>();
//    transformers.put(
//        named("read").and(takesArguments(0)).and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadNoArgs");
//    transformers.put(
//        named("read")
//            .and(takesArguments(1))
//            .and(takesArgument(0, is(byte[].class)))
//            .and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArray");
//    transformers.put(
//        named("read")
//            .and(takesArguments(3))
//            .and(takesArgument(0, is(byte[].class)))
//            .and(takesArgument(1, is(int.class)))
//            .and(takesArgument(2, is(int.class)))
//            .and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArrayOffset");
//    transformers.put(
//        named("readAllBytes").and(takesArguments(0)).and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadAllBytes");
//    transformers.put(
//        named("readNBytes")
//            .and(takesArguments(0))
//            .and(takesArgument(0, is(byte[].class)))
//            .and(takesArgument(1, is(int.class)))
//            .and(takesArgument(2, is(int.class)))
//            .and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadNBytes");
//    transformers.put(
//        named("available").and(takesArguments(0)).and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_Available");
//
//    // ServletInputStream methods
//    transformers.put(
//        named("readLine")
//            .and(takesArguments(3))
//            .and(takesArgument(0, is(byte[].class)))
//            .and(takesArgument(1, is(int.class)))
//            .and(takesArgument(2, is(int.class)))
//            .and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$InputStream_ReadByteArray");
//    //     servlet 3.1 API, but since we do not call it directly muzzle
//    transformers.put(
//        named("isFinished").and(takesArguments(0)).and(isPublic()),
//        ServletInputStreamInstrumentation.class.getName() + "$ServletInputStream_IsFinished");
    return transformers;
  }
}
