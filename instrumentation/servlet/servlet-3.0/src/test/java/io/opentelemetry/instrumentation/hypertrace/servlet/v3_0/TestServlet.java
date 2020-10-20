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

package io.opentelemetry.instrumentation.hypertrace.servlet.v3_0;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestServlet extends HttpServlet {

  public static final String RESPONSE_BODY = "{\"key\": \"val\"}";
  public static final String RESPONSE_HEADER = "responseheader";
  public static final String RESPONSE_HEADER_VALUE = "responsevalue";

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) {
    resp.setStatus(200);
    resp.setContentType("application/json");
    resp.setHeader(RESPONSE_HEADER, RESPONSE_HEADER_VALUE);
    try {
      resp.getWriter().print(RESPONSE_BODY);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
