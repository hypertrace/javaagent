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

package org;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class WrappingFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) {}

  @Override
  public void destroy() {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;

    ReqWrapper reqWrapper = new ReqWrapper(httpServletRequest);
    RespWrapper respWrapper = new RespWrapper(httpServletResponse);
    chain.doFilter(reqWrapper, respWrapper);
  }

  static class ReqWrapper extends HttpServletRequestWrapper {

    private ServletInputStream servletInputStream;
    private BufferedReader bufferedReader;

    public ReqWrapper(HttpServletRequest request) {
      super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (servletInputStream == null) {
        servletInputStream = new DelegatingServletInputStream(super.getInputStream());
      }
      return servletInputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      if (bufferedReader == null) {
        bufferedReader = new DelegatingBufferedReader(super.getReader());
      }
      return bufferedReader;
    }
  }

  static class RespWrapper extends HttpServletResponseWrapper {

    private ServletOutputStream servletOutputStream;
    private PrintWriter printWriter;

    public RespWrapper(HttpServletResponse response) {
      super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      if (servletOutputStream == null) {
        servletOutputStream = new DelegatingServletOutputStream(super.getOutputStream());
      }
      return servletOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      if (printWriter == null) {
        printWriter = new DelegatingPrintWriter(super.getWriter());
      }
      return printWriter;
    }
  }
}
