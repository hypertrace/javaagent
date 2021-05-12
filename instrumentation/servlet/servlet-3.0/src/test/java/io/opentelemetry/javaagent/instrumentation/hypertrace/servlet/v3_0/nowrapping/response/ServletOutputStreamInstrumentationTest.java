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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletOutputStream;
import org.ServletStreamContextAccess;
import org.TestServletOutputStream;
import org.hypertrace.agent.core.bootstrap.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.bootstrap.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServletOutputStreamInstrumentationTest extends AbstractInstrumenterTest {

  private static final String BODY = "boobar";

  @Test
  public void write_single_byte() throws IOException {
    ServletOutputStream servletOutputStream = new TestServletOutputStream();
    BoundedByteArrayOutputStream buffer =
        BoundedBuffersFactory.createStream(StandardCharsets.UTF_8);
    ServletStreamContextAccess.addToOutputStreamContext(servletOutputStream, buffer);

    byte[] bytes = BODY.getBytes();
    for (int i = 0; i < BODY.length(); i++) {
      servletOutputStream.write(bytes[i]);
    }
    Assertions.assertEquals(BODY, buffer.toStringWithSuppliedCharset());
  }

  @Test
  public void write_arr() throws IOException {
    ServletOutputStream servletOutputStream = new TestServletOutputStream();
    BoundedByteArrayOutputStream buffer =
        BoundedBuffersFactory.createStream(StandardCharsets.UTF_8);
    ServletStreamContextAccess.addToOutputStreamContext(servletOutputStream, buffer);

    servletOutputStream.write(BODY.getBytes());
    Assertions.assertEquals(BODY, buffer.toStringWithSuppliedCharset());
  }

  @Test
  public void write_arr_offset() throws IOException {
    ServletOutputStream servletOutputStream = new TestServletOutputStream();
    BoundedByteArrayOutputStream buffer =
        BoundedBuffersFactory.createStream(StandardCharsets.UTF_8);
    ServletStreamContextAccess.addToOutputStreamContext(servletOutputStream, buffer);

    byte[] bytes = BODY.getBytes();
    servletOutputStream.write(bytes, 0, 2);
    servletOutputStream.write(bytes, 2, bytes.length - 2);
    Assertions.assertEquals(BODY, buffer.toStringWithSuppliedCharset());
  }

  @Test
  public void print_str() throws IOException {
    ServletOutputStream servletOutputStream = new TestServletOutputStream();
    BoundedByteArrayOutputStream buffer =
        BoundedBuffersFactory.createStream(StandardCharsets.UTF_8);
    ServletStreamContextAccess.addToOutputStreamContext(servletOutputStream, buffer);

    servletOutputStream.print(BODY);
    Assertions.assertEquals(BODY, buffer.toStringWithSuppliedCharset());
  }

  @Test
  public void println_str() throws IOException {
    ServletOutputStream servletOutputStream = new TestServletOutputStream();
    BoundedByteArrayOutputStream buffer =
        BoundedBuffersFactory.createStream(StandardCharsets.UTF_8);
    ServletStreamContextAccess.addToOutputStreamContext(servletOutputStream, buffer);

    servletOutputStream.println(BODY);
    Assertions.assertEquals(BODY + "\r\n", buffer.toStringWithSuppliedCharset());
  }
}
