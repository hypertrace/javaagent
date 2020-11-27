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

package io.opentelemetry.instrumentation.hypertrace.apache.httpclient.readall;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.hypertrace.apache.httpclient.InputStreamUtils;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.OnMethodExit;
import net.bytebuddy.asm.Advice.Return;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.hypertrace.agent.core.ContentTypeUtils;
import org.hypertrace.agent.core.GlobalContextHolder;
import org.hypertrace.agent.core.HypertraceSemanticAttributes;

@AutoService(InstrumentationModule.class)
public class ApacheClientReadAllInstrumentationModule extends InstrumentationModule {

  public ApacheClientReadAllInstrumentationModule() {
    super("httpclient-readall");
  }

  @Override
  protected boolean defaultEnabled() {
    return false;
  }

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "io.opentelemetry.instrumentation.hypertrace.apache.httpclient.InputStreamUtils"
    };
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(
        new ApacheClientReadAllInstrumentationModule.ApacheClientInstrumentation(),
        new ApacheClientReadAllInstrumentationModule.HttpEntityInstrumentation());
  }

  /**
   * 1. instrumentation reads all response input stream before response is returned from the client
   *
   * <p>2. the original input stream is put into a global map along with buffered input stream. The
   * input stream instrumentation then uses the buffered stream.
   *
   * <p>3. InputStream HttpEntity.getContent() can be called only once so this method is
   * instrumented to allow the second call in the user application.
   */
  static class ApacheClientInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return implementsInterface(named("org.apache.http.client.HttpClient"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArguments(1))
              .and(takesArgument(0, named("org.apache.http.client.methods.HttpUriRequest"))),
          HttpClient_ExecuteAdvice.class.getName());

      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArguments(2))
              .and(takesArgument(0, named("org.apache.http.client.methods.HttpUriRequest")))
              .and(takesArgument(1, named("org.apache.http.protocol.HttpContext"))),
          HttpClient_ExecuteAdvice.class.getName());

      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArguments(2))
              .and(takesArgument(0, named("org.apache.http.client.methods.HttpUriRequest")))
              .and(takesArgument(1, named("org.apache.http.client.ResponseHandler"))),
          HttpClient_ExecuteAdvice.class.getName());

      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArguments(3))
              .and(takesArgument(0, named("org.apache.http.client.methods.HttpUriRequest")))
              .and(takesArgument(1, named("org.apache.http.client.ResponseHandler")))
              .and(takesArgument(2, named("org.apache.http.protocol.HttpContext"))),
          HttpClient_ExecuteAdvice.class.getName());

      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArguments(2))
              .and(takesArgument(0, named("org.apache.http.HttpHost")))
              .and(takesArgument(1, named("org.apache.http.HttpRequest"))),
          HttpClient_ExecuteAdvice.class.getName());

      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArguments(3))
              .and(takesArgument(0, named("org.apache.http.HttpHost")))
              .and(takesArgument(1, named("org.apache.http.HttpRequest")))
              .and(takesArgument(2, named("org.apache.http.protocol.HttpContext"))),
          HttpClient_ExecuteAdvice.class.getName());

      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArguments(3))
              .and(takesArgument(0, named("org.apache.http.HttpHost")))
              .and(takesArgument(1, named("org.apache.http.HttpRequest")))
              .and(takesArgument(2, named("org.apache.http.client.ResponseHandler"))),
          HttpClient_ExecuteAdvice.class.getName());

      transformers.put(
          isMethod()
              .and(named("execute"))
              .and(not(isAbstract()))
              .and(takesArguments(4))
              .and(takesArgument(0, named("org.apache.http.HttpHost")))
              .and(takesArgument(1, named("org.apache.http.HttpRequest")))
              .and(takesArgument(2, named("org.apache.http.client.ResponseHandler")))
              .and(takesArgument(3, named("org.apache.http.protocol.HttpContext"))),
          HttpClient_ExecuteAdvice.class.getName());
      return transformers;
    }
  }

  static class HttpClient_ExecuteAdvice {
    @OnMethodExit(suppress = Throwable.class)
    public static void exit(@Return Object response) {
      if (response instanceof HttpResponse) {
        Span currentSpan = Java8BytecodeBridge.currentSpan();
        HttpResponse httpResponse = (HttpResponse) response;
        HttpEntity entity = httpResponse.getEntity();

        Header contentType = entity.getContentType();
        if (contentType == null || !ContentTypeUtils.shouldCapture(contentType.getValue())) {
          return;
        }

        try {
          InputStream inputStream = entity.getContent();
          long contentSize = entity.getContentLength();
          if (contentSize <= 0 || contentSize == Long.MAX_VALUE) {
            contentSize = 128;
          }
          byte[] bodyBytes = InputStreamUtils.readToArr(inputStream, (int) contentSize);
          System.out.printf("Captured response body: %s\n", new String(bodyBytes));
          currentSpan.setAttribute(
              HypertraceSemanticAttributes.HTTP_RESPONSE_BODY.getKey(), new String(bodyBytes));
          ByteArrayInputStream bufferedInputStream = new ByteArrayInputStream(bodyBytes);

          GlobalContextHolder.objectMap.put(entity, inputStream);
          GlobalContextHolder.inputStreamMap.put(inputStream, bufferedInputStream);
        } catch (IOException e) {
          // TODO log
          e.printStackTrace();
        }
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
          ApacheClientReadAllInstrumentationModule.HttpEntity_GetContentAdvice.class.getName());
      return transformers;
    }
  }

  static class HttpEntity_GetContentAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.This HttpEntity thizz,
        @Advice.Return(readOnly = false) InputStream inputStream,
        @Advice.Thrown(readOnly = false) Throwable exception) {
      if (exception instanceof IllegalStateException) {
        Object originalInputStream = GlobalContextHolder.objectMap.get(thizz);
        if (originalInputStream != null) {
          // TODO race condition
          GlobalContextHolder.objectMap.remove(thizz);
          inputStream = (InputStream) originalInputStream;
          exception = null;
        }
      }
    }
  }
}
