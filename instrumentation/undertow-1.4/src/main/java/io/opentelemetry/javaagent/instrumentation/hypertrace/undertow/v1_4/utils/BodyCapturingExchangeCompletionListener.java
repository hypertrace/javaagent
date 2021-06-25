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

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import java.io.UnsupportedEncodingException;
import org.hypertrace.agent.core.instrumentation.HypertraceSemanticAttributes;
import org.hypertrace.agent.core.instrumentation.SpanAndBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BodyCapturingExchangeCompletionListener implements ExchangeCompletionListener {

  private static final Logger log =
      LoggerFactory.getLogger(BodyCapturingExchangeCompletionListener.class);

  private final SpanAndBuffer spanAndBuffer;

  public BodyCapturingExchangeCompletionListener(final SpanAndBuffer spanAndBuffer) {
    this.spanAndBuffer = spanAndBuffer;
  }

  @Override
  public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
    final String body;
    try {
      body = spanAndBuffer.byteArrayBuffer.toStringWithSuppliedCharset();
    } catch (UnsupportedEncodingException e) {
      log.error("illegal encoding", e);
      return;
    }
    spanAndBuffer.span.setAttribute(HypertraceSemanticAttributes.HTTP_REQUEST_BODY, body);
    spanAndBuffer.span.setAttribute("org.hypertrace.undertow.request_body_capture", "true");
    nextListener.proceed();
  }
}
