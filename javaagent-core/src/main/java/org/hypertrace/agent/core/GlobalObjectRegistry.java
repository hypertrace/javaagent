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

package org.hypertrace.agent.core;

import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class GlobalObjectRegistry {

  // original input stream to span and byte buffer
  public static final WeakConcurrentMap<InputStream, SpanAndBuffer> inputStreamToSpanAndBufferMap =
      new WeakConcurrentMap<>(false);

  // original output stream to byte buffer
  public static final WeakConcurrentMap<OutputStream, ByteArrayOutputStream>
      outputStreamToBufferMap = new WeakConcurrentMap<>(false);

  // original input stream to buffered one
  public static final WeakConcurrentMap<InputStream, InputStream> inputStreamMap =
      new WeakConcurrentMap<>(false);
  public static final WeakConcurrentMap<Object, Object> objectMap = new WeakConcurrentMap<>(false);

  public static class SpanAndBuffer {
    public final Span span;
    public final ByteArrayOutputStream byteArrayBuffer;
    public final AttributeKey<String> attributeKey;
    public final Charset charset;

    public SpanAndBuffer(
        Span span,
        ByteArrayOutputStream byteArrayBuffer,
        AttributeKey<String> attributeKey,
        Charset charset) {
      this.span = span;
      this.byteArrayBuffer = byteArrayBuffer;
      this.attributeKey = attributeKey;
      this.charset = charset;
    }
  }
}
