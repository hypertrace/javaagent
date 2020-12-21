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
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.is;
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
import java.io.UnsupportedEncodingException;
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
import org.hypertrace.agent.config.Config.AgentConfig;
import org.hypertrace.agent.core.BoundedByteArrayOutputStreamFactory;
import org.hypertrace.agent.core.ContentEncodingUtils;
import org.hypertrace.agent.core.ContentLengthUtils;
import org.hypertrace.agent.core.ContentTypeUtils;
import org.hypertrace.agent.core.GlobalObjectRegistry;
import org.hypertrace.agent.core.GlobalObjectRegistry.SpanAndBuffer;
import org.hypertrace.agent.core.HypertraceConfig;
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
          ApacheClientInstrumentationModule.class.getName() + "$HttpClient_ExecuteAdvice_response");

      // instrument request
      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArgument(0, hasSuperType(named("org.apache.http.HttpMessage")))),
          ApacheClientInstrumentationModule.class.getName() + "$HttpClient_ExecuteAdvice_request0");
      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArgument(1, hasSuperType(named("org.apache.http.HttpMessage")))),
          ApacheClientInstrumentationModule.class.getName() + "$HttpClient_ExecuteAdvice_request1");

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
      ApacheHttpClientUtils.traceRequest(Java8BytecodeBridge.currentSpan(), request);
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
      ApacheHttpClientUtils.traceRequest(Java8BytecodeBridge.currentSpan(), request);
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
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean enter() {
      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(HttpResponse.class);
      if (callDepth > 0) {
        return false;
      }
      return true;
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.Return Object response, @Advice.Enter boolean returnFromEnter) {
      if (!returnFromEnter) {
        return;
      }

      CallDepthThreadLocalMap.reset(HttpResponse.class);
      if (response instanceof HttpResponse) {
        HttpResponse httpResponse = (HttpResponse) response;
        Span currentSpan = Java8BytecodeBridge.currentSpan();
        AgentConfig agentConfig = HypertraceConfig.get();
        if (agentConfig.getDataCapture().getHttpHeaders().getResponse().getValue()) {
          ApacheHttpClientUtils.addResponseHeaders(currentSpan, httpResponse.headerIterator());
        }

        if (agentConfig.getDataCapture().getHttpBody().getResponse().getValue()) {
          HttpEntity entity = httpResponse.getEntity();
          ApacheHttpClientUtils.traceEntity(
              currentSpan, HypertraceSemanticAttributes.HTTP_RESPONSE_BODY.getKey(), entity);
        }
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

      // instrumentation for request body along with OutputStream instrumentation
      transformers.put(
          named("writeTo").and(takesArguments(1)).and(takesArgument(0, is(OutputStream.class))),
          ApacheClientInstrumentationModule.class.getName() + "$HttpEntity_WriteToAdvice");

      // instrumentation for response body along with InputStream instrumentation
      transformers.put(
          named("getContent").and(takesArguments(0)).and(returns(InputStream.class)),
          ApacheClientInstrumentationModule.class.getName() + "$HttpEntity_GetContentAdvice");
      return transformers;
    }
  }

  static class HttpEntity_GetContentAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.This HttpEntity thizz, @Advice.Return InputStream inputStream) {
      // here the Span.current() is finished for response entities
      Span clientSpan = ApacheHttpClientObjectRegistry.entityToSpan.get(thizz);
      // HttpEntity might be wrapped multiple times
      // this ensures that the advice runs only for the most outer one
      // the returned inputStream is put into globally accessible map
      // The InputStream instrumentation then checks if the input stream is in the map and only
      // then intercepts the reads.
      if (clientSpan == null) {
        return;
      }
      System.out.println("\n\n GetContnet advice");

      Header contentType = thizz.getContentType();
      System.out.println(contentType);
      if (contentType == null || !ContentTypeUtils.shouldCapture(contentType.getValue())) {
        System.out.println("\n\n GetContnet advice -- return");
        return;
      }
      System.out.println("\n\n GetContnet advice -- AAA");

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
              BoundedByteArrayOutputStreamFactory.create((int) contentSize),
              HypertraceSemanticAttributes.HTTP_RESPONSE_BODY,
              charset);
      System.out.println("\n\n GetContent advice -- End");
      System.out.println(inputStream);
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
      ByteArrayOutputStream byteArrayOutputStream =
          BoundedByteArrayOutputStreamFactory.create((int) contentSize);

      GlobalObjectRegistry.outputStreamToBufferMap.put(outputStream, byteArrayOutputStream);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This HttpEntity thizz, @Advice.Argument(0) OutputStream outputStream) {
      Span clientSpan = ApacheHttpClientObjectRegistry.entityToSpan.get(thizz);
      if (clientSpan == null) {
        return;
      }

      String encoding =
          thizz.getContentEncoding() != null ? thizz.getContentEncoding().getValue() : "";
      Charset charset = ContentEncodingUtils.toCharset(encoding);

      ByteArrayOutputStream bufferedOutStream =
          GlobalObjectRegistry.outputStreamToBufferMap.remove(outputStream);
      try {
        String requestBody = bufferedOutStream.toString(charset.name());
        System.out.printf("Captured request body via outputstream: %s\n", requestBody);
        clientSpan.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, requestBody);
      } catch (UnsupportedEncodingException e) {
        // should not happen, the charset has been parsed before
      }
    }
  }
}
