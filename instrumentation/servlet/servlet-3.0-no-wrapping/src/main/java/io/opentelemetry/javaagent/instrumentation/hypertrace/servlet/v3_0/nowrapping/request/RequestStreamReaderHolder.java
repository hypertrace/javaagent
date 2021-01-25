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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping.request;

import io.opentelemetry.api.trace.Span;
import java.io.BufferedReader;
import javax.servlet.ServletInputStream;

public class RequestStreamReaderHolder {

  private final Span span;
  private ServletInputStream servletInputStream;
  private BufferedReader bufferedReader;

  public RequestStreamReaderHolder(Span span) {
    this.span = span;
  }

  public Span getSpan() {
    return span;
  }

  public ServletInputStream getServletInputStream() {
    return servletInputStream;
  }

  public void setServletInputStream(ServletInputStream servletInputStream) {
    this.servletInputStream = servletInputStream;
  }

  public BufferedReader getBufferedReader() {
    return bufferedReader;
  }

  public void setBufferedReader(BufferedReader bufferedReader) {
    this.bufferedReader = bufferedReader;
  }
}
