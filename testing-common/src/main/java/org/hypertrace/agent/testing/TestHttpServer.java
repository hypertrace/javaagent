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

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;

public class TestHttpServer implements AutoCloseable {
  private final Server server = new Server(0);
  private final HandlerList handlerList = new HandlerList();

  public void start() throws Exception {
    HandlerList handlerList = new HandlerList();
    handlerList.addHandler(new GetNoContentHandler());
    handlerList.addHandler(new GetJsonHandler());
    handlerList.addHandler(new GetPlainTextHandler());
    handlerList.addHandler(new PostHandler());
    handlerList.addHandler(new PostRedirect());
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
      response.setHeader("test-response-header", "test-value");
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

  public static class GetPlainTextHandler extends ResponseTestHeadersHandler {
    public static final String RESPONSE_BODY = "name: james";

    @Override
    public void handle(
        String target,
        Request baseRequest,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {
      super.handle(target, baseRequest, request, response);

      if (target.equals("/get_plain_text") && "get".equalsIgnoreCase(request.getMethod())) {
        response.setStatus(200);
        response.setContentType("tExt/pLain");
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
}
