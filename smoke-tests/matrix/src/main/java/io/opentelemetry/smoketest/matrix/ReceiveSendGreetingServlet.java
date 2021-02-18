package io.opentelemetry.smoketest.matrix;

import com.google.gson.Gson;
import io.opentelemetry.smoketest.matrix.model.Greeting;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

public class ReceiveSendGreetingServlet extends HttpServlet {

  private final Gson gson = new Gson();

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String input = request.getReader().lines().collect(Collectors.joining());
    System.out.println("Received input: " + input);
    Greeting inputGreeting = gson.fromJson(input, Greeting.class);
    System.out.println("inputGreeting: " + inputGreeting);
    Greeting returnGreeting = new Greeting("Welcome ", inputGreeting.getName());
    System.out.println("returnGreeting: " + returnGreeting);
    String returnGreetingString = this.gson.toJson(returnGreeting);
    PrintWriter out = response.getWriter();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    out.print(returnGreetingString);
    out.flush();
  }
}
