package io.opentelemetry.smoketest.matrix;

import io.opentelemetry.smoketest.matrix.util.StreamTransferUtil;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

public class EchoServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String path = (request.getContextPath() + "/headers").replace("//", "/");
    URL url = new URL("http", "localhost", request.getLocalPort(), path);
    URLConnection urlConnection = url.openConnection();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (InputStream remoteInputStream = urlConnection.getInputStream()) {

      //set ContentType & CharacterEncoding
      response.setContentType(request.getContentType());
      response.setCharacterEncoding(request.getCharacterEncoding());

      //get & set headers
      long bytesRead = StreamTransferUtil.transfer(remoteInputStream, buffer);
      String headers = buffer.toString("UTF-8");
      String headerResponseBody =
          Base64.getEncoder().encodeToString((bytesRead + " bytes read by " + urlConnection.getClass().getName() +
              "\n" + headers).getBytes());
      response.setHeader("Header-Dump", headerResponseBody);

      //get set echo body
      ServletInputStream inputStream = request.getInputStream();
      // read the body without waiting for -1
      // e.g. Jackson reads the body without waiting for returning -1
      byte[] bodyBytes = new byte[1500000];
      inputStream.read(bodyBytes);
      response.getOutputStream().write(bodyBytes);
    }
  }
}
