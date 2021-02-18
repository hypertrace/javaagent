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

package io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpclient.v4_0;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.instrumentation.hypertrace.apachehttpclient.v4_0.ApacheHttpClientObjectRegistry.SpanAndAttributeKey;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.hypertrace.agent.core.instrumentation.GlobalObjectRegistry;
import org.hypertrace.agent.core.instrumentation.GlobalObjectRegistry.SpanAndBuffer;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.utils.ContentLengthUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeCharsetUtils;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeUtils;

public class HttpEntityInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return implementsInterface(named("org.apache.http.HttpEntity"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();

    // instrumentation for request body along with OutputStream instrumentation
    transformers.put(
        named("writeTo").and(takesArguments(1)).and(takesArgument(0, is(OutputStream.class))),
        HttpEntityInstrumentation.class.getName() + "$HttpEntity_WriteToAdvice");

    // instrumentation for response body along with InputStream instrumentation
    transformers.put(
        named("getContent").and(takesArguments(0)).and(returns(InputStream.class)),
        HttpEntityInstrumentation.class.getName() + "$HttpEntity_GetContentAdvice");
    return transformers;
  }

  static class HttpEntity_GetContentAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.This HttpEntity thizz, @Advice.Return InputStream inputStream) {
      // here the Span.current() is finished for response entities
      // TODO the entry from map is nto explicitly removed, It could be done by instrumenting
      // CloseableHttpResponse
      // instroduced in version 4.3
      SpanAndAttributeKey clientSpan = ApacheHttpClientObjectRegistry.entityToSpan.get(thizz);
      // HttpEntity might be wrapped multiple times
      // this ensures that the advice runs only for the most outer one
      // the returned inputStream is put into globally accessible map
      // The InputStream instrumentation then checks if the input stream is in the map and only
      // then intercepts the reads.
      if (clientSpan == null) {
        return;
      }

      long contentSize = thizz.getContentLength();
      if (contentSize <= 0 || contentSize == Long.MAX_VALUE) {
        contentSize = ContentLengthUtils.DEFAULT;
      }

      Header contentTypeHeader = thizz.getContentType();
      String charsetStr = null;
      if (contentTypeHeader != null) {
        charsetStr = ContentTypeUtils.parseCharset(contentTypeHeader.getValue());
      }
      Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);

      SpanAndBuffer spanAndBuffer =
          new SpanAndBuffer(
              clientSpan.span,
              BoundedBuffersFactory.createStream((int) contentSize, charset),
              clientSpan.attributeKey,
              charset);
      GlobalObjectRegistry.inputStreamToSpanAndBufferMap.put(inputStream, spanAndBuffer);
    }
  }

  static class HttpEntity_WriteToAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This HttpEntity thizz, @Advice.Argument(0) OutputStream outputStream) {

      if (ApacheHttpClientObjectRegistry.entityToSpan.get(thizz) == null) {
        return;
      }

      long contentSize = thizz.getContentLength();
      if (contentSize <= 0 || contentSize == Long.MAX_VALUE) {
        contentSize = ContentLengthUtils.DEFAULT;
      }

      Header contentTypeHeader = thizz.getContentType();
      String charsetStr = null;
      if (contentTypeHeader != null) {
        charsetStr = ContentTypeUtils.parseCharset(contentTypeHeader.getValue());
      }
      Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);

      BoundedByteArrayOutputStream byteArrayOutputStream =
          BoundedBuffersFactory.createStream((int) contentSize, charset);

      GlobalObjectRegistry.outputStreamToBufferMap.put(outputStream, byteArrayOutputStream);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This HttpEntity thizz, @Advice.Argument(0) OutputStream outputStream) {
      SpanAndAttributeKey spanAndAttributeKey =
          ApacheHttpClientObjectRegistry.entityToSpan.remove(thizz);
      if (spanAndAttributeKey == null) {
        return;
      }

      BoundedByteArrayOutputStream bufferedOutStream =
          GlobalObjectRegistry.outputStreamToBufferMap.remove(outputStream);
      try {
        String requestBody = bufferedOutStream.toStringWithSuppliedCharset();
        spanAndAttributeKey.span.setAttribute(spanAndAttributeKey.attributeKey, requestBody);
      } catch (UnsupportedEncodingException e) {
        // should not happen, the charset has been parsed before
      }
    }
  }
}
