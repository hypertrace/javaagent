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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.nowrapping;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestServlets {

  public static final String RESPONSE_BODY = "{\"key\": \"val\"}";

  public static final String RESPONSE_HEADER = "responseheader";
  public static final String RESPONSE_HEADER_VALUE = "responsevalue";

  public static class EchoStream_single_byte extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getInputStream().read() != -1) {}
      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);

      byte[] response_bodyBytes = RESPONSE_BODY.getBytes();
      for (int i = 0; i < RESPONSE_BODY.length(); i++) {
        resp.getOutputStream().write(response_bodyBytes[i]);
      }
    }
  }

  public static class EchoStream_arr extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getInputStream().read(new byte[2]) != -1) {}
      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);
      resp.getOutputStream().write(RESPONSE_BODY.getBytes());
    }
  }

  public static class EchoStream_arr_offset extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getInputStream().read(new byte[12], 3, 2) != -1) {}
      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);

      byte[] responseBytes = RESPONSE_BODY.getBytes();
      resp.getOutputStream().write(responseBytes, 0, 2);
      resp.getOutputStream().write(responseBytes, 2, 1);
      resp.getOutputStream().write(responseBytes, 3, responseBytes.length - 3);
    }
  }

  public static class EchoStream_readLine_print extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getInputStream().readLine(new byte[14], 3, 3) != -1) {}
      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);
      resp.getOutputStream().print(RESPONSE_BODY);
    }
  }

  public static class EchoWriter extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getReader().read() != -1) {}

      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);
      resp.getWriter().print(RESPONSE_BODY);
    }
  }
}
