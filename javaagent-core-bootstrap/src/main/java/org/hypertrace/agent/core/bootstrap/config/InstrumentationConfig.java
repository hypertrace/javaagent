package org.hypertrace.agent.core.bootstrap.config;


public interface InstrumentationConfig {

  Message httpHeaders();
  Message httpBody();
  Message rpcMetadata();
  Message rpcBody();

  interface Message {
    boolean request();
    boolean response();
  }
}
