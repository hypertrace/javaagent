package io.opentelemetry.instrumentation.hypertrace.servlet.v3_0;

/**
 * @author Pavol Loffay
 */
public class DynamicConfig {

  public static String getProperty(String name){
    return System.getenv(name);
  }
}
