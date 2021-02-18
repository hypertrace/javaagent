package io.opentelemetry.smoketest.matrix.model;

public class Greeting {

  String greeting;
  String name;

  public Greeting(String greeting, String name) {
    this.greeting = greeting;
    this.name = name;
  }

  public String getGreeting() {
    return greeting;
  }

  public void setGreeting(String greeting) {
    this.greeting = greeting;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Greeting{" +
        "greeting='" + greeting + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
