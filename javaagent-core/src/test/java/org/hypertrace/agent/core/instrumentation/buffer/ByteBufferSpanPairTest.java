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

package org.hypertrace.agent.core.instrumentation.buffer;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.hypertrace.agent.core.TriFunction;
import org.hypertrace.agent.core.filter.FilterResult;
import org.hypertrace.agent.core.instrumentation.HypertraceEvaluationException;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ByteBufferSpanPairTest {

  private static final String MULTIPART = "multipart/form-data; boundary=b";
  private static final FilterResult ALLOW = new FilterResult(false, 0, null);

  private final Map<String, Object> attributes = new HashMap<>();
  private final AtomicInteger filterCalls = new AtomicInteger();
  private final AtomicInteger filterSeenBodyLength = new AtomicInteger(-1);

  /** Minimal recording Span: javaagent-core has no OpenTelemetry SDK test dependency. */
  private Span recordingSpan() {
    return (Span)
        Proxy.newProxyInstance(
            Span.class.getClassLoader(),
            new Class<?>[] {Span.class},
            (proxy, method, args) -> {
              if (method.getName().equals("setAttribute") && args.length == 2) {
                Object key = args[0];
                attributes.put(
                    key instanceof AttributeKey ? ((AttributeKey<?>) key).getKey() : (String) key,
                    args[1]);
              }
              return method.getReturnType() == Span.class ? proxy : null;
            });
  }

  private ByteBufferSpanPair pair(
      String contentType, TriFunction<Span, String, Map<String, String>, FilterResult> filter) {
    // not BoundedBuffersFactory: its static init needs the agent's InstrumentationConfig
    BoundedByteArrayOutputStream buffer =
        new BoundedByteArrayOutputStream(1024, StandardCharsets.ISO_8859_1);
    buffer.setMultipartFormData(ContentTypeUtils.isMultipartFormData(contentType));
    return new ByteBufferSpanPair(recordingSpan(), buffer, filter, new HashMap<>());
  }

  private TriFunction<Span, String, Map<String, String>, FilterResult> allowAll() {
    return (span, body, headers) -> {
      filterCalls.incrementAndGet();
      filterSeenBodyLength.set(body.length());
      return ALLOW;
    };
  }

  private static String base64(byte[] bytes) {
    return Base64.getEncoder().withoutPadding().encodeToString(bytes);
  }

  @Test
  void multipartBodyIsCapturedAsBase64OfRawBytes() throws Exception {
    // a binary file part: bytes that are not valid text in any charset
    byte[] body =
        concat(
            "--b\r\nContent-Disposition: form-data; name=\"f\"; filename=\"a.bin\"\r\n\r\n"
                .getBytes(StandardCharsets.US_ASCII),
            new byte[] {0, (byte) 0xff, (byte) 0xfe, 0x1, (byte) 0x80},
            "\r\n--b--\r\n".getBytes(StandardCharsets.US_ASCII));
    ByteBufferSpanPair pair = pair(MULTIPART, allowAll());
    pair.writeToBuffer(body);

    pair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);

    Assertions.assertEquals(base64(body), attributes.get("http.request.body.base64"));
    Assertions.assertFalse(attributes.containsKey("http.request.body"));
    Assertions.assertEquals(1, filterCalls.get());
  }

  @Test
  void nonMultipartBodyIsStillCapturedAsText() throws Exception {
    ByteBufferSpanPair pair = pair("application/json", allowAll());
    pair.writeToBuffer("{\"a\":1}".getBytes(StandardCharsets.UTF_8));

    pair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);

    Assertions.assertEquals("{\"a\":1}", attributes.get("http.request.body"));
    Assertions.assertFalse(attributes.containsKey("http.request.body.base64"));
  }

  @Test
  void malformedMultipartBodiesDoNotThrowAndAreCapturedVerbatim() throws Exception {
    byte[][] malformed = {
      // not multipart at all
      "this is not multipart".getBytes(StandardCharsets.US_ASCII),
      // boundary never terminated, part truncated mid-header
      "--b\r\nContent-Disposition: form-da".getBytes(StandardCharsets.US_ASCII),
      // wrong boundary
      "--zzz\r\n\r\nhello\r\n--zzz--".getBytes(StandardCharsets.US_ASCII),
      // random binary garbage
      new byte[] {(byte) 0xde, (byte) 0xad, 0, (byte) 0xbe, (byte) 0xef, (byte) 0xff},
    };
    for (byte[] body : malformed) {
      attributes.clear();
      ByteBufferSpanPair pair = pair(MULTIPART, allowAll());
      pair.writeToBuffer(body);

      Assertions.assertDoesNotThrow(
          () -> pair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY),
          Arrays.toString(body));

      Assertions.assertEquals(base64(body), attributes.get("http.request.body.base64"));
    }
  }

  @Test
  void emptyMultipartBodySetsNoAttribute() {
    ByteBufferSpanPair pair = pair(MULTIPART, allowAll());

    Assertions.assertDoesNotThrow(
        () -> pair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));

    Assertions.assertTrue(attributes.isEmpty());
    Assertions.assertEquals(1, filterCalls.get());
  }

  @Test
  void multipartBodyIsStillEvaluatedAndCanBeBlocked() throws Exception {
    ByteBufferSpanPair pair =
        pair(MULTIPART, (span, body, headers) -> new FilterResult(true, 403, "blocked"));
    pair.writeToBuffer("--b\r\n\r\nattack\r\n--b--".getBytes(StandardCharsets.US_ASCII));

    HypertraceEvaluationException e =
        Assertions.assertThrows(
            HypertraceEvaluationException.class,
            () -> pair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY));
    Assertions.assertEquals(403, e.getFilterResult().getBlockingStatusCode());
  }

  @Test
  void multipartBodyIsTruncatedAtMaxSizeOnRawBytes() throws Exception {
    BoundedByteArrayOutputStream buffer =
        new BoundedByteArrayOutputStream(4, StandardCharsets.ISO_8859_1);
    buffer.setMultipartFormData(true);
    ByteBufferSpanPair pair =
        new ByteBufferSpanPair(recordingSpan(), buffer, allowAll(), new HashMap<>());
    pair.writeToBuffer(new byte[] {1, 2, 3, 4, 5, 6});

    pair.captureBody(HypertraceSemanticAttributes.HTTP_REQUEST_BODY);

    Assertions.assertEquals(
        base64(new byte[] {1, 2, 3, 4}), attributes.get("http.request.body.base64"));
  }

  private static byte[] concat(byte[]... parts) {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    for (byte[] part : parts) {
      out.write(part, 0, part.length);
    }
    return out.toByteArray();
  }
}
