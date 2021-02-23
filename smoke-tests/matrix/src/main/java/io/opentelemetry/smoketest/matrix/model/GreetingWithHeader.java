package io.opentelemetry.smoketest.matrix.model;

public class GreetingWithHeader {
  
  String header;
  String greeting;

  public GreetingWithHeader(String header, String greeting) {
    this.header = header;
    this.greeting = greeting;
  }

  public String getHeader() {
    return header;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public String getGreeting() {
    return greeting;
  }

  public void setGreeting(String greeting) {
    this.greeting = greeting;
  }

  @Override
  public String toString() {
    return "GreetingWithHeader{" +
        "header='" + header + '\'' +
        ", greeting=" + greeting +
        '}';
  }
}
