/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.smoketest.matrix;

import io.opentelemetry.smoketest.matrix.util.StreamTransferUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GreetingServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String path = (req.getContextPath() + "/headers").replace("//", "/");
    URL url = new URL("http", "localhost", req.getLocalPort(), path);
    URLConnection urlConnection = url.openConnection();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (InputStream remoteInputStream = urlConnection.getInputStream()) {
      long bytesRead = StreamTransferUtil.transfer(remoteInputStream, buffer);
      String responseBody = buffer.toString("UTF-8");
      ServletOutputStream outputStream = resp.getOutputStream();
      outputStream.print(
          bytesRead + " bytes read by " + urlConnection.getClass().getName() + "\n" + responseBody);
      outputStream.flush();
    }
  }
}
