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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.v3_0.rw;

import java.io.IOException;
import java.util.stream.Stream;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestServlets {

  public static final String RESPONSE_BODY = "{\"key\": \"val\"}";

  public static final String RESPONSE_HEADER = "responseheader";
  public static final String RESPONSE_HEADER_VALUE = "responsevalue";

  public static class GetHello extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getReader().read() != -1) {}
      resp.setStatus(204);
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);
      resp.getWriter().write("hello");
    }
  }

  public static class EchoWriter_single_char extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getReader().read() != -1) {}

      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);

      for (int i = 0; i < RESPONSE_BODY.length(); i++)
        resp.getWriter().write(RESPONSE_BODY.charAt(i));
    }
  }

  public static class EchoWriter_arr extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getReader().read(new char[2]) != -1) {}

      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);
      resp.getWriter().write(RESPONSE_BODY.toCharArray());
    }
  }

  public static class EchoWriter_arr_offset extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getReader().read(new char[12], 3, 2) != -1) {}

      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);

      char[] chars = RESPONSE_BODY.toCharArray();
      resp.getWriter().write(chars, 0, 2);
      resp.getWriter().write(chars, 2, 2);
      resp.getWriter().write(chars, 4, chars.length - 4);
    }
  }

  public static class EchoWriter_readLine_write extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getReader().readLine() != null) {}

      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);

      resp.getWriter().write(RESPONSE_BODY);
    }
  }

  public static class EchoWriter_readLines extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      Stream<String> lines = req.getReader().lines();
      lines.forEach(s -> {});

      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);

      resp.getWriter().write(RESPONSE_BODY);
    }
  }

  public static class EchoWriter_readLine_print_str extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getReader().readLine() != null) {}

      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);

      resp.getWriter().print(RESPONSE_BODY);
    }
  }

  public static class EchoWriter_readLine_print_arr extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      while (req.getReader().readLine() != null) {}

      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);

      resp.getWriter().print(RESPONSE_BODY.toCharArray());
    }
  }

  public static class EchoAsyncResponse_writer extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {

      AsyncContext asyncContext = req.startAsync();
      asyncContext.start(
          () -> {
            while (true) {
              try {
                if (!(req.getReader().read() != -1)) break;
              } catch (IOException e) {
                e.printStackTrace();
              }
            }

            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            HttpServletResponse httpServletResponse =
                (HttpServletResponse) asyncContext.getResponse();
            httpServletResponse.setStatus(200);
            httpServletResponse.setContentType("application/json");
            httpServletResponse.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);
            try {
              httpServletResponse.getWriter().print(RESPONSE_BODY);
            } catch (IOException e) {
              e.printStackTrace();
            }
            asyncContext.complete();
          });
    }
  }

  public static class EchoStream_read_large_array extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      req.getInputStream().read(new byte[1000], 0, 1000);

      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);

      resp.getWriter().print(RESPONSE_BODY.toCharArray());
    }
  }

  public static class EchoReader_read_large_array extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      req.getReader().read(new char[1000], 0, 1000);

      resp.setStatus(200);
      resp.setContentType("application/json");
      resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);

      resp.getWriter().print(RESPONSE_BODY.toCharArray());
    }
  }
}
