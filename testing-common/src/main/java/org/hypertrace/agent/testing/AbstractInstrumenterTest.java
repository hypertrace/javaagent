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

package org.hypertrace.agent.testing;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.opentelemetry.api.trace.Tracer;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

/**
 * Abstract test class that tests {@link //
 * io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule} on the classpath.
 */
public abstract class AbstractInstrumenterTest {

  private static final org.slf4j.Logger log =
      LoggerFactory.getLogger(AbstractInstrumenterTest.class);

  /**
   * For test runs, agent's global tracer will report to this list writer.
   *
   * <p>Before the start of each test the reported traces will be reset.
   */
  public static final TestOtlpReceiver TEST_WRITER = new TestOtlpReceiver();

  static {
    // TODO causes Caused by: java.lang.ClassCastException
    ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
    ((Logger) LoggerFactory.getLogger("io.opentelemetry")).setLevel(Level.DEBUG);
  }

  private static final int DEFAULT_TIMEOUT_SECONDS = 30;
  protected OkHttpClient httpClient =
      new OkHttpClient.Builder()
          .callTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .build();

  @AfterAll
  public static void closeServer() throws Exception {
    TEST_WRITER.close();
  }

  @BeforeAll
  public static void beforeAll() throws Exception {
    TEST_WRITER.start();
  }

  @BeforeEach
  public void beforeEach() {
    TEST_WRITER.clear();
  }

  public static RequestBody requestBody(
      final boolean chunked, final long size, final int writeSize) {
    final byte[] buffer = new byte[writeSize];
    Arrays.fill(buffer, (byte) 'x');

    return new RequestBody() {
      @Override
      public MediaType contentType() {
        return MediaType.get("application/json; charset=utf-8");
      }

      @Override
      public long contentLength() throws IOException {
        return chunked ? -1L : size;
      }

      @Override
      public void writeTo(BufferedSink sink) throws IOException {
        sink.write("{\"body\":\"".getBytes());
        for (int count = 0; count < size; count += writeSize) {
          sink.write(buffer, 0, (int) Math.min(size - count, writeSize));
        }
        sink.write("\"}".getBytes());
      }
    };
  }

  public static RequestBody blockedRequestBody(
      final boolean chunked, final long size, final int writeSize) {
    final byte[] buffer = new byte[writeSize];
    Arrays.fill(buffer, (byte) 'x');

    return new RequestBody() {
      @Override
      public MediaType contentType() {
        return MediaType.get("application/json; charset=utf-8");
      }

      @Override
      public long contentLength() throws IOException {
        return chunked ? -1L : size;
      }

      @Override
      public void writeTo(BufferedSink sink) throws IOException {
        sink.write("{\"block=true\":\"".getBytes());
        for (int count = 0; count < size; count += writeSize) {
          sink.write(buffer, 0, (int) Math.min(size - count, writeSize));
        }
        sink.write("\"}".getBytes());
      }
    };
  }
}
