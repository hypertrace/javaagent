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

package org.hypertrace.agent.otel.extensions;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for SpanUtils. */
public class SpanUtilsTest {

  /**
   * We can't seem to create mocks using Mockito in this environment, so we create a concrete
   * implementation of the Span interface, to serve as a mock.
   */
  private static class MockSpan implements Span {

    private final Map<AttributeKey<String>, String> attributeMap = new HashMap<>();

    @Override
    public <T> Span setAttribute(AttributeKey<T> key, T value) {
      this.attributeMap.put((AttributeKey<String>) key, (String) value);
      return this;
    }

    @Override
    public Span addEvent(String name, Attributes attributes) {
      return null;
    }

    @Override
    public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
      return null;
    }

    @Override
    public Span setStatus(StatusCode statusCode, String description) {
      return null;
    }

    @Override
    public Span recordException(Throwable exception, Attributes additionalAttributes) {
      return null;
    }

    @Override
    public Span updateName(String name) {
      return null;
    }

    @Override
    public void end() {}

    @Override
    public void end(long timestamp, TimeUnit unit) {}

    @Override
    public SpanContext getSpanContext() {
      return null;
    }

    @Override
    public boolean isRecording() {
      return false;
    }

    String getAttribute(String attributeKey) {
      AttributeKey key = AttributeKey.stringKey(attributeKey);
      return attributeMap.get(key);
    }
  }

  private static final String AGENT_VERSION = "1.2.3";

  private MockSpan mockSpan;

  public SpanUtilsTest() {
    super();
  }

  @BeforeEach
  public void setup() {
    mockSpan = new MockSpan();
  }

  @Test
  public void testSetSpanAttributes() {
    SpanUtils.setVersionSupplier(() -> AGENT_VERSION);

    SpanUtils.setSpanAttributes(mockSpan);

    String attrValue = mockSpan.getAttribute(SpanUtils.AGENT_VERSION_ATTRIBUTE_KEY);

    Assertions.assertEquals(AGENT_VERSION, attrValue);
  }
}
