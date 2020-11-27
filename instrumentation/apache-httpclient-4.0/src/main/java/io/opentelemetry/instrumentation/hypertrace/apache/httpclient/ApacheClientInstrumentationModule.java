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

package io.opentelemetry.instrumentation.hypertrace.apache.httpclient;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.hypertrace.agent.core.ContentEncodingUtils;
import org.hypertrace.agent.core.ContentLengthUtils;
import org.hypertrace.agent.core.ContentTypeUtils;
import org.hypertrace.agent.core.GlobalObjectRegistry;
import org.hypertrace.agent.core.GlobalObjectRegistry.SpanAndBuffer;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;

@AutoService(InstrumentationModule.class)
public class ApacheClientInstrumentationModule extends InstrumentationModule {

  public ApacheClientInstrumentationModule() {
    super(ApacheHttpClientInstrumentationName.PRIMARY, ApacheHttpClientInstrumentationName.OTHER);
  }

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      packageName + ".ApacheHttpClientObjectRegistry", packageName + ".ApacheHttpClientUtils",
    };
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(new HttpEntityInstrumentation(), new ApacheClientInstrumentation());
  }

  static class ApacheClientInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return implementsInterface(named("org.apache.http.client.HttpClient"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();

      // instrument response
      transformers.put(
          isMethod().and(named("execute")).and(not(isAbstract())),
          HttpClient_ExecuteAdvice_response.class.getName());

      // instrument request
      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArgument(0, hasSuperType(named("org.apache.http.HttpMessage")))),
          HttpClient_ExecuteAdvice_request0.class.getName());
      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArgument(1, hasSuperType(named("org.apache.http.HttpMessage")))),
          HttpClient_ExecuteAdvice_request1.class.getName());

      return transformers;
    }
  }

  static class HttpClient_ExecuteAdvice_request0 {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean enter(@Advice.Argument(0) HttpMessage request) {
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(HttpMessage.class);
      if (callDepth > 0) {
        return false;
      }
      System.out.println("\non enter");
      ApacheHttpClientUtils.traceRequest(request);
      return true;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exit(
        @Advice.Enter boolean returnFromEnter, @Advice.Thrown Throwable throwable) {
      if (returnFromEnter) {
        CallDepthThreadLocalMap.reset(HttpMessage.class);
      }
    }
  }

  static class HttpClient_ExecuteAdvice_request1 {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean enter(@Advice.Argument(1) HttpMessage request) {
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(HttpMessage.class);
      if (callDepth > 0) {
        return false;
      }
      System.out.println("\non enter");
      ApacheHttpClientUtils.traceRequest(request);
      return true;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exit(
        @Advice.Enter boolean returnFromEnter, @Advice.Thrown Throwable throwable) {
      if (returnFromEnter) {
        CallDepthThreadLocalMap.reset(HttpMessage.class);
      }
    }
  }

  static class HttpClient_ExecuteAdvice_response {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.Return Object response) {
      System.out.println("exit advice");
      if (response instanceof HttpResponse) {
        HttpResponse httpResponse = (HttpResponse) response;
        Span currentSpan = Java8BytecodeBridge.currentSpan();
        ApacheHttpClientUtils.addResponseHeaders(currentSpan, httpResponse.headerIterator());

        HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
          return;
        }
        // TODO check entity.isRepeatable() and read the full body
        ApacheHttpClientUtils.traceEntity(currentSpan, entity);
      } else {
        System.out.println("\n\nIt is not HttpResponse #execute");
      }
    }
  }

  static class HttpEntityInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      return implementsInterface(named("org.apache.http.HttpEntity"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
      transformers.put(
          named("getContent").and(takesArguments(0)).and(returns(InputStream.class)),
          HttpEntity_GetContentAdvice.class.getName());
      transformers.put(
          named("writeTo")
              .and(takesArguments(1))
              .and(takesArgument(0, named("java.io.OutputStream"))),
          HttpEntity_WriteToAdvice.class.getName());
      return transformers;
    }
  }

  static class HttpEntity_GetContentAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.This HttpEntity thizz, @Advice.Return InputStream inputStream) {
      // here the Span.current() is finished for response entities
      System.out.println("\n\n GetContent");
      Span clientSpan = ApacheHttpClientObjectRegistry.objectToSpanMap.remove(thizz);
      // HttpEntity might be wrapped multiple times
      // this ensures that the advice runs only for the most outer one
      // the returned inputStream is put into globally accessible map
      // The InputStream instrumentation then checks if the input stream is in the map and only
      // then intercepts the reads.
      if (clientSpan == null) {
        return;
      }

      Header contentType = thizz.getContentType();
      if (contentType == null || !ContentTypeUtils.shouldCapture(contentType.getValue())) {
        return;
      }

      long contentSize = thizz.getContentLength();
      if (contentSize <= 0 || contentSize == Long.MAX_VALUE) {
        contentSize = ContentLengthUtils.DEFAULT;
      }

      String encoding =
          thizz.getContentEncoding() != null ? thizz.getContentEncoding().getValue() : "";
      Charset charset = ContentEncodingUtils.toCharset(encoding);
      SpanAndBuffer spanAndBuffer =
          new SpanAndBuffer(
              clientSpan,
              new ByteArrayOutputStream((int) contentSize),
              HypertraceSemanticAttributes.HTTP_RESPONSE_BODY,
              charset);
      GlobalObjectRegistry.objectToSpanAndBufferMap.put(inputStream, spanAndBuffer);
    }
  }

  static class HttpEntity_WriteToAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void enter(
        @Advice.This HttpEntity thizz, @Advice.Argument(0) OutputStream outputStream) {
      if (!ApacheHttpClientObjectRegistry.objectToSpanMap.containsKey(thizz)) {
        return;
      }

      // TODO proceed only if the entity wasn't read
      System.out.println("\n\n writeTo\n\n");
      System.out.println(thizz.getClass().getName());
      long contentSize = thizz.getContentLength();
      if (contentSize <= 0 || contentSize == Long.MAX_VALUE) {
        contentSize = ContentLengthUtils.DEFAULT;
      }
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((int) contentSize);

      GlobalObjectRegistry.objectMap.put(outputStream, byteArrayOutputStream);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This HttpEntity thizz, @Advice.Argument(0) OutputStream outputStream) {
      Span clientSpan = ApacheHttpClientObjectRegistry.objectToSpanMap.remove(thizz);
      if (clientSpan == null) {
        return;
      }

      String encoding =
          thizz.getContentEncoding() != null ? thizz.getContentEncoding().getValue() : "";
      Charset charset = ContentEncodingUtils.toCharset(encoding);

      ByteArrayOutputStream bufferedOutStream =
          (ByteArrayOutputStream) GlobalObjectRegistry.objectMap.remove(outputStream);
      byte[] bodyBytes = bufferedOutStream.toByteArray();
      String body = new String(bodyBytes, charset);
      System.out.printf("request body via outputstream: %s\n", body);
      // TODO add to span
      //      InputStreamUtils.addAttribute(spanAndBuffer.span, spanAndBuffer.attributeKey, body);
    }
  }
}
