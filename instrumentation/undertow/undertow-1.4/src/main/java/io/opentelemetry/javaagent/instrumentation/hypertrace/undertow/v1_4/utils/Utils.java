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

package io.opentelemetry.javaagent.instrumentation.hypertrace.undertow.v1_4.utils;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.undertow.UndertowHttpServerTracer;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import org.hypertrace.agent.core.config.InstrumentationConfig.ConfigProvider;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.SpanAndBuffer;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;
import org.xnio.channels.StreamSourceChannel;

public final class Utils {

  /**
   * Creates a {@link SpanAndBuffer} and stores it in the {@link
   * io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext} with the {@link
   * StreamSourceChannel} as the key so that later invocations of {@link StreamSourceChannel#read}
   *
   * @param httpServerExchange that {@link HttpServerExchange#getRequestChannel()} was invoked on
   * @param returnedChannel {@link StreamSourceChannel} implementation created and held by {@link
   *     HttpServerExchange} for the request body
   * @param contextStore to hold the {@link SpanAndBuffer} associated with the {@link
   *     StreamSourceChannel} returned by {@link HttpServerExchange#getRequestChannel()}
   */
  public static void createAndStoreBufferForSpan(
      final HttpServerExchange httpServerExchange,
      final StreamSourceChannel returnedChannel,
      final ContextStore<StreamSourceChannel, SpanAndBuffer> contextStore) {
    if (!ConfigProvider.get().httpBody().request()
        || !ContentTypeUtils.shouldCapture(
            httpServerExchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE))) {
      return;
    }
    final Span span =
        Span.fromContext(UndertowHttpServerTracer.tracer().getServerContext(httpServerExchange));
    final Charset charset = Charset.forName(httpServerExchange.getRequestCharset());
    final BoundedByteArrayOutputStream boundedByteArrayOutputStream =
        BoundedBuffersFactory.createStream(
            (int) httpServerExchange.getRequestContentLength(), charset);
    final SpanAndBuffer spanAndBuffer =
        new SpanAndBuffer(
            span,
            boundedByteArrayOutputStream,
            HypertraceSemanticAttributes.HTTP_REQUEST_BODY,
            charset);
    contextStore.put(returnedChannel, spanAndBuffer);
    httpServerExchange.addExchangeCompleteListener(
        new BodyCapturingExchangeCompletionListener(spanAndBuffer));
  }

  /**
   * @param readOnlyBuffer that was just read into {@link StreamSourceChannel#read(ByteBuffer)}
   * @param numBytesRead into the provided {@link ByteBuffer} by {@link
   *     StreamSourceChannel#read(ByteBuffer)}
   * @param spanAndBuffer named tuple retrieved from the {@link
   *     io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext} where the {@link
   *     StreamSourceChannel} instance is the key. This was put into the {@link
   *     io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext} by {@link
   *     io.opentelemetry.javaagent.instrumentation.hypertrace.undertow.v1_4.UndertowHttpServerExchangeInstrumentation}
   *     the first time {@link HttpServerExchange#getRequestChannel()} was invoked on the request
   */
  public static void handleRead(
      final ByteBuffer readOnlyBuffer, final int numBytesRead, final SpanAndBuffer spanAndBuffer) {
    if (numBytesRead <= 0) {
      return;
    }
    readOnlyBuffer.position(readOnlyBuffer.position() - numBytesRead);

    final BoundedByteArrayOutputStream boundedByteArrayOutputStream = spanAndBuffer.byteArrayBuffer;
    for (int i = 0; i < numBytesRead; i++) {
      final byte b = readOnlyBuffer.get();
      boundedByteArrayOutputStream.write(b);
    }
  }
}
