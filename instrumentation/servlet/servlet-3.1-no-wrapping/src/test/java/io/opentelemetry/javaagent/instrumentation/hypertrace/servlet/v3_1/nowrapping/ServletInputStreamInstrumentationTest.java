package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_1.nowrapping;

import io.opentelemetry.api.trace.Span;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import org.ServletInputStreamContextAccess;
import org.TestInputStream;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ServletInputStreamInstrumentationTest extends AbstractInstrumenterTest {

  private static final String TEST_SPAN_NAME = "foo";
  private static final String BODY = "boobar";

  @Test
  public void read() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    ServletInputStream servletInputStream = new TestInputStream(new ByteArrayInputStream(BODY.getBytes()));

    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
    BoundedByteArrayOutputStream buffer = BoundedBuffersFactory.createStream(StandardCharsets.UTF_8);
    Metadata metadata = new Metadata(span, httpServletRequest, buffer);
    ServletInputStreamContextAccess.addToContext(servletInputStream, metadata);

    while (servletInputStream.read() != -1) {}
    Assertions.assertEquals(BODY, buffer.toStringWithSuppliedCharset());
    System.out.println(buffer.toStringWithSuppliedCharset());
  }

  @Test
  public void read_callDepth_is_cleared() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    ServletInputStream servletInputStream = new TestInputStream(new ByteArrayInputStream(BODY.getBytes()));
    servletInputStream.read();

    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
    BoundedByteArrayOutputStream buffer = BoundedBuffersFactory.createStream(StandardCharsets.UTF_8);
    Metadata metadata = new Metadata(span, httpServletRequest, buffer);
    ServletInputStreamContextAccess.addToContext(servletInputStream, metadata);

    while (servletInputStream.read() != -1) {}
    Assertions.assertEquals(BODY.substring(1), buffer.toStringWithSuppliedCharset());
    System.out.println(buffer.toStringWithSuppliedCharset());
  }

  @Test
  public void read_call_depth_read_calls_read() throws IOException {
    Span span = TEST_TRACER.spanBuilder(TEST_SPAN_NAME).startSpan();

    ServletInputStream servletInputStream = new TestInputStream(new ByteArrayInputStream(BODY.getBytes()));
    servletInputStream.read(new byte[2]);

    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
    BoundedByteArrayOutputStream buffer = BoundedBuffersFactory.createStream(StandardCharsets.UTF_8);
    Metadata metadata = new Metadata(span, httpServletRequest, buffer);
    ServletInputStreamContextAccess.addToContext(servletInputStream, metadata);

    servletInputStream.read(new byte[BODY.length()]);
    Assertions.assertEquals(BODY.substring(2), buffer.toStringWithSuppliedCharset());
    System.out.println(buffer.toStringWithSuppliedCharset());
  }
}
