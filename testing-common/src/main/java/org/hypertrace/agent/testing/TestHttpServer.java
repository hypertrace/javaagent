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

package org.hypertrace.agent.testing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;

public class TestHttpServer implements AutoCloseable {

  public static final String RESPONSE_HEADER_NAME = "test-response-header";
  public static final String RESPONSE_HEADER_VALUE = "test-value";

  private final Server server = new Server(0);
  private final HandlerList handlerList = new HandlerList();

  public void start() throws Exception {
    HandlerList handlerList = new HandlerList();
    handlerList.addHandler(new GetNoContentHandler());
    handlerList.addHandler(new GetJsonHandler());
    handlerList.addHandler(new PostHandler());
    handlerList.addHandler(new PostRedirect());
    handlerList.addHandler(new EchoHandler());
    server.setHandler(handlerList);
    server.start();
  }

  public void addHandler(Handler handler) {
    this.handlerList.addHandler(handler);
  }

  @Override
  public void close() throws Exception {
    server.stop();
  }

  public int port() {
    return server.getConnectors()[0].getLocalPort();
  }

  static class ResponseTestHeadersHandler extends AbstractHandler {
    @Override
    public void handle(
        String target,
        Request baseRequest,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {
      response.setHeader(RESPONSE_HEADER_NAME, RESPONSE_HEADER_VALUE);
    }
  }

  static class GetNoContentHandler extends ResponseTestHeadersHandler {
    @Override
    public void handle(
        String target,
        Request baseRequest,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {
      super.handle(target, baseRequest, request, response);

      if (target.equals("/get_no_content") && "get".equalsIgnoreCase(request.getMethod())) {
        response.setStatus(204);
        baseRequest.setHandled(true);
      }
    }
  }

  public static class GetJsonHandler extends ResponseTestHeadersHandler {
    public static final String RESPONSE_BODY = "{\"name\": \"james\"}";

    @Override
    public void handle(
        String target,
        Request baseRequest,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {
      super.handle(target, baseRequest, request, response);

      if (target.equals("/get_json") && "get".equalsIgnoreCase(request.getMethod())) {
        response.setStatus(200);
        response.setContentType("aPplication/jSon");
        response.getWriter().print(RESPONSE_BODY);
        baseRequest.setHandled(true);
      }
    }
  }

  static class PostHandler extends ResponseTestHeadersHandler {
    @Override
    public void handle(
        String target,
        Request baseRequest,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {
      super.handle(target, baseRequest, request, response);

      if (target.equals("/post") && "post".equalsIgnoreCase(request.getMethod())) {
        // read the input stream to for sending on the client side
        ServletInputStream inputStream = request.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        while ((nRead = inputStream.read()) != -1) {
          buffer.write((byte) nRead);
        }
        System.out.printf("Test server received: %s\n", buffer.toString());

        response.setStatus(204);
        baseRequest.setHandled(true);
      }
    }
  }

  static class PostRedirect extends ResponseTestHeadersHandler {
    @Override
    public void handle(
        String target,
        Request baseRequest,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {
      super.handle(target, baseRequest, request, response);

      if (target.equals("/post_redirect_to_get_no_content")
          && "post".equalsIgnoreCase(request.getMethod())) {
        response.sendRedirect(
            String.format("http://localhost:%d/get_no_content", baseRequest.getServerPort()));
        baseRequest.setHandled(true);
      }
    }
  }

  static class EchoHandler extends ResponseTestHeadersHandler {
    @Override
    public void handle(
        String target,
        Request baseRequest,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {
      super.handle(target, baseRequest, request, response);

      if (target.equals("/echo") && "post".equalsIgnoreCase(request.getMethod())) {
        ServletInputStream inputStream = request.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        while ((nRead = inputStream.read()) != -1) {
          buffer.write((byte) nRead);
        }

        response.setStatus(200);
        response.setContentType(request.getContentType());
        response.getWriter().print(buffer.toString());
        baseRequest.setHandled(true);
      }
    }
  }
}
