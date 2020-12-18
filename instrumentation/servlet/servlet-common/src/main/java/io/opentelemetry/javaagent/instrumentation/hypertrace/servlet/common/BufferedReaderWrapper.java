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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.common;

import java.io.BufferedReader;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BufferedReaderWrapper extends BufferedReader {

  private static final Logger logger = LoggerFactory.getLogger(BufferedReaderWrapper.class);

  private final BufferedReader reader;
  private final CharBufferData charBufferData;

  public BufferedReaderWrapper(BufferedReader reader, CharBufferData charBufferData) {
    super(reader);
    this.reader = reader;
    this.charBufferData = charBufferData;
  }

  @Override
  public int read() throws IOException {
    int read = this.reader.read();
    try {
      if (read >= 0) {
        charBufferData.appendData(read);
      }
    } catch (Exception e) {
      logger.error("Error in read() - ", e);
    }
    return read;
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    int read = this.reader.read(cbuf, off, len);
    try {
      if (read > 0) {
        charBufferData.appendData(cbuf, off, off + read);
      }
    } catch (Exception e) {
      logger.error("Error in read(char[] cbuf, int off, int len) - ", e);
    }
    return read;
  }

  @Override
  public String readLine() throws IOException {
    String read = this.reader.readLine();
    if (read == null) {
      return null;
    } else {
      try {
        charBufferData.appendData(read);
      } catch (Exception e) {
        logger.error("Error in readLine() - ", e);
      }
      return read;
    }
  }

  @Override
  public long skip(long n) throws IOException {
    return this.reader.skip(n);
  }

  @Override
  public boolean ready() throws IOException {
    return this.reader.ready();
  }

  @Override
  public boolean markSupported() {
    return this.reader.markSupported();
  }

  @Override
  public void mark(int readAheadLimit) throws IOException {
    this.reader.mark(readAheadLimit);
  }

  @Override
  public void reset() throws IOException {
    this.reader.reset();
  }

  @Override
  public void close() throws IOException {
    this.reader.close();
  }

  @Override
  public int read(java.nio.CharBuffer target) throws IOException {
    int initPos = target.position();
    int read = this.reader.read(target);
    try {
      if (read > 0) {
        for (int i = initPos; i < initPos + read; ++i) {
          charBufferData.appendData(target.get(i));
        }
      }
    } catch (Exception e) {
      logger.error("Error in read(target) - ", e);
    }
    return read;
  }

  @Override
  public int read(char[] cbuf) throws IOException {
    int read = this.reader.read(cbuf);
    try {
      if (read > 0) {
        charBufferData.appendData(cbuf, 0, read);
      }
    } catch (Exception e) {
      logger.error("Error in read(char[]) - ", e);
    }
    return read;
  }
}
