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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v2_3;

import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.common.BufferedWriterWrapper;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.common.ByteBufferData;
import io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.common.CharBufferData;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.hypertrace.agent.core.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BufferingHttpServletResponse extends HttpServletResponseWrapper {

  private static final Logger logger = LoggerFactory.getLogger(BufferingHttpServletRequest.class);

  private CharBufferData charBufferData;
  private ByteBufferData byteBufferData;

  private ServletOutputStream outputStream = null;
  private PrintWriter writer = null;
  private final Map<String, List<String>> headers = new LinkedHashMap<>();
  private String contentType;

  public BufferingHttpServletResponse(HttpServletResponse httpServletResponse) {
    super(httpServletResponse);
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (outputStream == null) {
      outputStream = super.getOutputStream();
      if (shouldReadContent()) {
        Charset charset = Charset.forName(StandardCharsets.ISO_8859_1.name());
        String encoding = this.getCharacterEncoding();
        try {
          if (encoding != null) {
            charset = Charset.forName(encoding);
          } else {
            logger.debug(
                "Encoding is not specified in servlet request will default to [ISO-8859-1]");
          }
        } catch (IllegalArgumentException var5) {
          logger.warn("Encoding [{}] not recognized. Will default to [ISO-8859-1]", encoding);
        }
        getByteBuffer().setCharset(charset);

        outputStream = new BufferingServletOutputStream(outputStream, getByteBuffer());
      }
    }
    return outputStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (writer == null) {
      writer = super.getWriter();
      if (shouldReadContent()) {
        writer = new PrintWriter(new BufferedWriterWrapper(writer, getCharBuffer()));
      }
    }
    return writer;
  }

  @Override
  public void setDateHeader(String name, long date) {
    super.setDateHeader(name, date);
    safelyCaptureHeader(name, date);
  }

  @Override
  public void addDateHeader(String name, long date) {
    super.addDateHeader(name, date);
    safelyCaptureHeader(name, date);
  }

  @Override
  public void setHeader(String name, String value) {
    super.setHeader(name, value);
    safelyCaptureHeader(name, value);
  }

  @Override
  public void addHeader(String name, String value) {
    super.addHeader(name, value);
    safelyCaptureHeader(name, value);
  }

  @Override
  public void setIntHeader(String name, int value) {
    super.setIntHeader(name, value);
    safelyCaptureHeader(name, value);
  }

  @Override
  public void addIntHeader(String name, int value) {
    super.addIntHeader(name, value);
    safelyCaptureHeader(name, value);
  }

  @Override
  public void addCookie(Cookie cookie) {
    super.addCookie(cookie);
    safelyAddCookieHeader(cookie);
  }

  @Override
  public void setContentType(String type) {
    super.setContentType(type);
    this.contentType = type;
  }

  private void safelyAddCookieHeader(Cookie cookie) {
    try {
      HttpCookie httpCookie = new HttpCookie(cookie.getName(), cookie.getValue());
      httpCookie.setComment(cookie.getComment());
      httpCookie.setDomain(cookie.getDomain());
      httpCookie.setPath(cookie.getPath());
      httpCookie.setMaxAge(cookie.getMaxAge());
      httpCookie.setVersion(cookie.getVersion());
      httpCookie.setSecure(cookie.getSecure());
      String headerValue = httpCookie.toString();
      List<String> values = new ArrayList<>(1);
      values.add(headerValue);
      headers.put("Set-Cookie", values);
    } catch (Exception e) {
      logger.error("Error capturing cookie - ", e);
    }
  }

  private void safelyCaptureHeader(String name, Object value) {
    try {
      List<String> values = headers.getOrDefault(name, new ArrayList<>());
      values.add(String.valueOf(value));
      headers.put(name, values);
    } catch (Exception e) {
      logger.error("Error capturing header - ", e);
    }
  }

  public Map<String, List<String>> getBufferedHeaders() {
    return headers;
  }

  public synchronized ByteBufferData getByteBuffer() {
    if (null == byteBufferData) {
      byteBufferData = new ByteBufferData();
    }
    return byteBufferData;
  }

  public synchronized CharBufferData getCharBuffer() {
    if (charBufferData == null) {
      charBufferData = new CharBufferData();
    }
    return charBufferData;
  }

  public String getBufferAsString() {
    if (byteBufferData != null) {
      return byteBufferData.getBufferAsString();
    }
    if (charBufferData != null) {
      return charBufferData.getBufferAsString();
    }
    return null;
  }

  private boolean shouldReadContent() {
    if (contentType == null || contentType.isEmpty()) {
      return false;
    }
    return ContentTypeUtils.shouldCapture(contentType);
  }

  public static class BufferingServletOutputStream extends ServletOutputStream {

    private static final Logger logger =
        LoggerFactory.getLogger(BufferingServletOutputStream.class);

    private final ServletOutputStream outputStream;
    private final ByteBufferData byteBufferData;

    public BufferingServletOutputStream(
        ServletOutputStream outputStream, ByteBufferData byteBufferData) {
      this.outputStream = outputStream;
      this.byteBufferData = byteBufferData;
    }

    @Override
    public void write(int b) throws IOException {
      outputStream.write(b);
      try {
        byteBufferData.appendData(b);
      } catch (Exception e) {
        logger.error("Error in write(int b) ", e);
      }
    }

    public void write(byte[] b) throws IOException {
      write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      outputStream.write(b, off, len);
      try {
        byteBufferData.appendData(b, off, len);
      } catch (Exception e) {
        logger.error("Error in write(byte[] b, int off, int len) ", e);
      }
    }

    @Override
    public void flush() throws IOException {
      outputStream.flush();
    }

    @Override
    public void close() throws IOException {
      outputStream.close();
    }
  }
}
