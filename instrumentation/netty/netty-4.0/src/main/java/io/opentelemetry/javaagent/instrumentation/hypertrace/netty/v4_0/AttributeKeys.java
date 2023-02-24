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

import io.netty.util.AttributeKey;
import java.util.Map;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;

public class AttributeKeys {
  public static final AttributeKey<BoundedByteArrayOutputStream> RESPONSE_BODY_BUFFER =
      io.opentelemetry.javaagent.instrumentation.netty.v4_0.AttributeKeys.attributeKey(
          HypertraceSemanticAttributes.HTTP_RESPONSE_BODY.getKey());

  public static final AttributeKey<BoundedByteArrayOutputStream> REQUEST_BODY_BUFFER =
      io.opentelemetry.javaagent.instrumentation.netty.v4_0.AttributeKeys.attributeKey(
          HypertraceSemanticAttributes.HTTP_REQUEST_BODY.getKey());

  public static final AttributeKey<Map<String, String>> REQUEST_HEADERS =
      io.opentelemetry.javaagent.instrumentation.netty.v4_0.AttributeKeys.attributeKey(
          AttributeKeys.class.getName() + ".request-headers");

  public static final AttributeKey<?> REQUEST =
          io.opentelemetry.javaagent.instrumentation.netty.v4_0.AttributeKeys.attributeKey(
                  "io.opentelemetry.javaagent.instrumentation.netty.v4_0.AttributeKeys.http-server-request");
}
