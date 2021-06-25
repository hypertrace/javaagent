package io.opentelemetry.javaagent.instrumentation.hypertrace.jsf;

public class GreetingForm {

  String name = "";
  String message = "";


  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  String getMessage() {
    return message;
  }

  void submit() {
    message = "Hello " + name;
    if (name.equals("exception")) {
      throw new RuntimeException("submit exception");
    }
  }

}
