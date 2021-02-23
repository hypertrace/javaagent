package io.opentelemetry.smoketest.matrix;

import com.google.gson.Gson;
import io.opentelemetry.smoketest.matrix.model.GreetingWithHeader;
import io.opentelemetry.smoketest.matrix.util.StreamTransferUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Collectors;

public class ReceiveSendGreetingServlet extends HttpServlet {

  private final Gson gson = new Gson();

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String path = (request.getContextPath() + "/headers").replace("//", "/");
    URL url = new URL("http", "localhost", request.getLocalPort(), path);
    URLConnection urlConnection = url.openConnection();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (InputStream remoteInputStream = urlConnection.getInputStream()) {
      
      //get headers
      long bytesRead = StreamTransferUtil.transfer(remoteInputStream, buffer);
      String headerResponseBody = buffer.toString("UTF-8");
      headerResponseBody = bytesRead + " bytes read by " + urlConnection.getClass().getName() + "\n" + headerResponseBody;
      
      //get set echo body
      String input = request.getReader().lines().collect(Collectors.joining());
      GreetingWithHeader greetingWithHeader = new GreetingWithHeader(headerResponseBody, input);
      String returnGreetingHeaderString = this.gson.toJson(greetingWithHeader);
      
      //set response
      response.setContentType(request.getContentType());
      response.setCharacterEncoding(request.getCharacterEncoding());
      PrintWriter out = response.getWriter();
      out.print(returnGreetingHeaderString);
      out.flush();
    }
  }
}
