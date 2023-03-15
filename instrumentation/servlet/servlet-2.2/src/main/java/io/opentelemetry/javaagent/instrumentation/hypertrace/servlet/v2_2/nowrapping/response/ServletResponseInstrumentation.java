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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v2_2.nowrapping.response;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.*;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.field.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hypertrace.agent.core.config.InstrumentationConfig;
import org.hypertrace.agent.core.instrumentation.HypertraceCallDepthThreadLocalMap;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.SpanAndObjectPair;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedBuffersFactory;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedByteArrayOutputStream;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;
import org.hypertrace.agent.core.instrumentation.utils.ContentTypeCharsetUtils;

public class ServletResponseInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("javax.servlet.http.HttpServletResponse"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("getOutputStream")
            .and(takesArguments(0))
            .and(returns(named("javax.servlet.ServletOutputStream")))
            .and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$ServletResponse_getOutputStream");
    transformer.applyAdviceToMethod(
        named("getWriter").and(takesArguments(0)).and(returns(PrintWriter.class)).and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$ServletResponse_getWriter");
    transformer.applyAdviceToMethod(
        named("setContentType").and(takesArgument(0, String.class)).and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$ServletResponse_setContentType");
    transformer.applyAdviceToMethod(
        named("setContentLength").and(takesArgument(0, int.class)).and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$ServletResponse_setContentLength");
    transformer.applyAdviceToMethod(
        named("setCharacterEncoding").and(takesArgument(0, String.class)).and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$ServletResponse_setCharacterEncoding");
    transformer.applyAdviceToMethod(
        named("setStatus").and(takesArgument(0, int.class)).and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$HttpServletResponse_setStatus");
    transformer.applyAdviceToMethod(
        named("setHeader").and(takesArguments(2)).and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$HttpServletResponse_setHeader");
    transformer.applyAdviceToMethod(
        named("addHeader").and(takesArguments(2)).and(isPublic()),
        ServletResponseInstrumentation.class.getName() + "$HttpServletResponse_addHeader");
    for (String methodName : new String[] {"setDateHeader", "addDateHeader"}) {
      transformer.applyAdviceToMethod(
          named(methodName).and(takesArguments(2)).and(isPublic()),
          ServletResponseInstrumentation.class.getName() + "$HttpServletResponse_setDateHeader");
    }
    for (String methodName : new String[] {"setIntHeader", "addIntHeader"}) {
      transformer.applyAdviceToMethod(
          named(methodName).and(takesArguments(2)).and(isPublic()),
          ServletResponseInstrumentation.class.getName() + "$HttpServletResponse_setIntHeader");
    }
  }

  static class ServletResponse_setContentType {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.Argument(0) String type) {
      System.out.println("inside javax.servlet.ServletResponse.setContentType [" + type + "]");
    }
  }

  static class ServletResponse_setContentLength {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.Argument(0) int len) {
      System.out.println("inside javax.servlet.ServletResponse.setContentLength [" + len + "]");
    }
  }

  static class ServletResponse_setCharacterEncoding {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.Argument(0) String charset) {
      System.out.println(
          "inside javax.servlet.ServletResponse.setCharacterEncoding [" + charset + "]");
    }
  }

  @SuppressWarnings("unused")
  static class HttpServletResponse_setStatus {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.Argument(value = 0) int status) {
      System.out.println(
          "inside javax.servlet.http.HttpServletResponse.setStatus [" + status + "]");
    }
  }

  @SuppressWarnings("unused")
  static class HttpServletResponse_setHeader {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.Argument(value = 0) String headerName,
        @Advice.Argument(value = 1) String headerValue) {
      System.out.println(
          "inside javax.servlet.http.HttpServletResponse.setHeader ["
              + headerName
              + ", "
              + headerValue
              + "]");
      Java8BytecodeBridge.currentSpan()
          .setAttribute(HypertraceSemanticAttributes.httpResponseHeader(headerName), headerValue);
    }
  }

  @SuppressWarnings("unused")
  static class HttpServletResponse_addHeader {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.Argument(value = 0) String headerName,
        @Advice.Argument(value = 1) String headerValue) {
      System.out.println(
          "inside javax.servlet.http.HttpServletResponse.addHeader ["
              + headerName
              + ", "
              + headerValue
              + "]");
      Span currentSpan = Java8BytecodeBridge.currentSpan();
      if (!(currentSpan instanceof ReadableSpan)) {
        return;
      }
      AttributeKey<String> attributeKey =
          HypertraceSemanticAttributes.httpResponseHeader(headerName);
      String oldHeaderValue = ((ReadableSpan) currentSpan).getAttribute(attributeKey);
      if (oldHeaderValue == null) {
        currentSpan.setAttribute(attributeKey, headerValue);
      } else {
        currentSpan.setAttribute(attributeKey, String.join(",", oldHeaderValue, headerValue));
      }
    }
  }

  @SuppressWarnings("unused")
  static class HttpServletResponse_setDateHeader {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.Argument(value = 0) String headerName,
        @Advice.Argument(value = 1) long headerValue) {
      System.out.println(
          "inside javax.servlet.http.HttpServletResponse.setDateHeader ["
              + headerName
              + ", "
              + headerValue
              + "]");
      Java8BytecodeBridge.currentSpan()
          .setAttribute(
              HypertraceSemanticAttributes.httpResponseHeaderLong(headerName), headerValue);
    }
  }

  @SuppressWarnings("unused")
  static class HttpServletResponse_setIntHeader {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.Argument(value = 0) String headerName,
        @Advice.Argument(value = 1) int headerValue) {
      System.out.println(
          "inside javax.servlet.http.HttpServletResponse.setIntHeader ["
              + headerName
              + ", "
              + headerValue
              + "]");
      Java8BytecodeBridge.currentSpan()
          .setAttribute(
              HypertraceSemanticAttributes.httpResponseHeaderLong(headerName), headerValue);
    }
  }

  @SuppressWarnings("unused")
  static class ServletResponse_getOutputStream {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static HttpServletResponse enter(@Advice.This ServletResponse servletResponse) {
      System.out.println("start Enter javax.servlet.ServletResponse.getOutputStream");

      if (!(servletResponse instanceof HttpServletResponse)) {
        System.out.println("end1 Enter javax.servlet.ServletResponse.getOutputStream");
        return null;
      }
      // ignore wrappers, the filter/servlet instrumentation gets the captured body from context
      // store by using response as a key and the filter/servlet instrumentation runs early when
      // wrappers are not used.
      HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
      // the getReader method might call getInputStream
      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletResponse.class);
      System.out.println("end Enter javax.servlet.ServletResponse.getOutputStream");
      return httpServletResponse;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Enter HttpServletResponse httpServletResponse,
        @Advice.Thrown Throwable throwable,
        @Advice.Return ServletOutputStream servletOutputStream) {
      System.out.println("start Exit javax.servlet.ServletResponse.getOutputStream");

      if (httpServletResponse == null) {
        System.out.println("end1 Exit javax.servlet.ServletResponse.getOutputStream");
        return;
      }

      int callDepth = HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletResponse.class);
      if (callDepth > 0) {
        System.out.println("end2 Exit javax.servlet.ServletResponse.getOutputStream");
        return;
      }
      if (throwable != null) {
        System.out.println("end3 Exit javax.servlet.ServletResponse.getOutputStream");
        return;
      }

      VirtualField<ServletOutputStream, BoundedByteArrayOutputStream> contextStore =
          VirtualField.find(ServletOutputStream.class, BoundedByteArrayOutputStream.class);
      if (contextStore.get(servletOutputStream) != null) {
        // getOutputStream() can be called multiple times
        System.out.println("end4 Exit javax.servlet.ServletResponse.getOutputStream");
        return;
      }

      // do not capture if data capture is disabled or not supported content type
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      // TODO: capture body based on response content type
      if (instrumentationConfig.httpBody().response()) {
        String charsetStr = httpServletResponse.getCharacterEncoding();
        Charset charset = ContentTypeCharsetUtils.toCharset(charsetStr);
        BoundedByteArrayOutputStream buffer = BoundedBuffersFactory.createStream(charset);
        contextStore.set(servletOutputStream, buffer);
        SpanAndObjectPair spanAndObjectPair = new SpanAndObjectPair(null, null);
        spanAndObjectPair.setAssociatedObject(servletOutputStream);
        VirtualField.find(HttpServletResponse.class, SpanAndObjectPair.class)
            .set(httpServletResponse, spanAndObjectPair);
      }
      System.out.println("end Exit javax.servlet.ServletResponse.getOutputStream");
    }
  }

  @SuppressWarnings("unused")
  static class ServletResponse_getWriter {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static HttpServletResponse enter(@Advice.This ServletResponse servletResponse) {
      System.out.println("start Enter javax.servlet.ServletResponse.getWriter");

      if (!(servletResponse instanceof HttpServletResponse)) {
        System.out.println("end1 Enter javax.servlet.ServletResponse.getWriter");
        return null;
      }
      HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
      // the getWriter method might call getInputStream
      HypertraceCallDepthThreadLocalMap.incrementCallDepth(ServletResponse.class);
      System.out.println("end Enter javax.servlet.ServletResponse.getWriter");
      return httpServletResponse;
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(
        @Advice.Enter HttpServletResponse httpServletResponse,
        @Advice.Thrown Throwable throwable,
        @Advice.Return PrintWriter printWriter) {
      System.out.println("start Exit javax.servlet.ServletResponse.getWriter");
      if (httpServletResponse == null) {
        System.out.println("end1 Exit javax.servlet.ServletResponse.getWriter");
        return;
      }

      int callDepth = HypertraceCallDepthThreadLocalMap.decrementCallDepth(ServletResponse.class);
      if (callDepth > 0) {
        System.out.println("end2 Exit javax.servlet.ServletResponse.getWriter");
        return;
      }
      if (throwable != null) {
        System.out.println("end3 Exit javax.servlet.ServletResponse.getWriter");
        return;
      }

      VirtualField<PrintWriter, BoundedCharArrayWriter> contextStore =
          VirtualField.find(PrintWriter.class, BoundedCharArrayWriter.class);
      if (contextStore.get(printWriter) != null) {
        // getWriter() can be called multiple times
        System.out.println("end4 Exit javax.servlet.ServletResponse.getWriter");
        return;
      }

      // do not capture if data capture is disabled or not supported content type
      InstrumentationConfig instrumentationConfig = InstrumentationConfig.ConfigProvider.get();
      // TODO: capture body based on response content type
      if (instrumentationConfig.httpBody().response()) {
        BoundedCharArrayWriter writer = BoundedBuffersFactory.createWriter();
        contextStore.set(printWriter, writer);
        SpanAndObjectPair spanAndObjectPair = new SpanAndObjectPair(null, null);
        spanAndObjectPair.setAssociatedObject(printWriter);
        VirtualField.find(HttpServletResponse.class, SpanAndObjectPair.class)
            .set(httpServletResponse, spanAndObjectPair);
      }
      System.out.println("end Exit javax.servlet.ServletResponse.getWriter");
    }
  }
}
