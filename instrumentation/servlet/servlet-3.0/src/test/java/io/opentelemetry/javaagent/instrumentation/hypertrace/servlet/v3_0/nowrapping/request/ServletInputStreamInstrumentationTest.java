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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.request;

import io.opentelemetry.api.trace.Span;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import javax.servlet.ServletInputStream;
import org.ServletStreamContextAccess;
import org.TestServletInputStream;
import org.hypertrace.agent.core.QuadFunction;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.buffer.ByteBufferSpanPair;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServletInputStreamInstrumentationTest extends AbstractInstrumenterTest {

  private static final String TEST_SPAN_NAME = "foo";
  private static final String BODY = "boobar";

  @Test
  public void read() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    ServletInputStream servletInputStream =
        new TestServletInputStream(new ByteArrayInputStream(BODY.getBytes()));

    BoundedByteArrayOutputStream buffer =
        BoundedBuffersFactory.createStream(StandardCharsets.UTF_8);
    ByteBufferSpanPair bufferSpanPair =
        new ByteBufferSpanPair(
            span, buffer, NOOP_FILTER, Collections.emptyMap(), Collections.emptyMap());
    ServletStreamContextAccess.addToInputStreamContext(servletInputStream, bufferSpanPair);

    while (servletInputStream.read() != -1) {}
    Assertions.assertEquals(BODY, buffer.toStringWithSuppliedCharset());
  }

  @Test
  public void read_callDepth_is_cleared() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    ServletInputStream servletInputStream =
        new TestServletInputStream(new ByteArrayInputStream(BODY.getBytes()));
    servletInputStream.read();

    BoundedByteArrayOutputStream buffer =
        BoundedBuffersFactory.createStream(StandardCharsets.UTF_8);
    ByteBufferSpanPair bufferSpanPair =
        new ByteBufferSpanPair(
            span, buffer, NOOP_FILTER, Collections.emptyMap(), Collections.emptyMap());
    ServletStreamContextAccess.addToInputStreamContext(servletInputStream, bufferSpanPair);

    while (servletInputStream.read() != -1) {}
    Assertions.assertEquals(BODY.substring(1), buffer.toStringWithSuppliedCharset());
  }

  @Test
  public void read_call_depth_read_calls_read() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    ServletInputStream servletInputStream =
        new TestServletInputStream(new ByteArrayInputStream(BODY.getBytes()));
    servletInputStream.read(new byte[2]);

    BoundedByteArrayOutputStream buffer =
        BoundedBuffersFactory.createStream(StandardCharsets.UTF_8);
    ByteBufferSpanPair bufferSpanPair =
        new ByteBufferSpanPair(
            span, buffer, NOOP_FILTER, Collections.emptyMap(), Collections.emptyMap());
    ServletStreamContextAccess.addToInputStreamContext(servletInputStream, bufferSpanPair);

    servletInputStream.read(new byte[BODY.length()]);
    Assertions.assertEquals(BODY.substring(2), buffer.toStringWithSuppliedCharset());
  }

  private static final QuadFunction<Span, String, Map<String, String>, Map<String, String>, Boolean>
      NOOP_FILTER = (span, body, headers, missingAttrs) -> false;
}
