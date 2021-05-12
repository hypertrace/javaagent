package org.hypertrace.agent.core.bootstrap.config;

import org.hypertrace.agent.config.Config.DataCapture;

// TODO add @AutoService
public class HypertraceInstrumentationConfig implements InstrumentationConfig {

  private final ImmutableMessage httpHeaders;
  private final ImmutableMessage httpBody;
  private final ImmutableMessage rpcMetadata;
  private final ImmutableMessage rpcBody;

  public HypertraceInstrumentationConfig(DataCapture dataCapture) {
    this.httpHeaders = new ImmutableMessage(dataCapture.getHttpHeaders().hasRequest(), dataCapture.getHttpHeaders().hasResponse());
    this.httpBody = new ImmutableMessage(dataCapture.getHttpBody().hasRequest(), dataCapture.getHttpBody().hasResponse());
    this.rpcMetadata = new ImmutableMessage(dataCapture.getRpcMetadata().hasRequest(), dataCapture.getRpcMetadata().hasResponse());
    this.rpcBody = new ImmutableMessage(dataCapture.getRpcBody().hasRequest(), dataCapture.getRpcBody().hasResponse());
  }

  @Override
  public Message httpHeaders() {
    return httpHeaders;
  }

  @Override
  public Message httpBody() {
    return httpBody;
  }

  @Override
  public Message rpcMetadata() {
    return rpcMetadata;
  }

  @Override
  public Message rpcBody() {
    return rpcBody;
  }

  private class ImmutableMessage implements Message {

    private final boolean request;
    private final boolean response;

    public ImmutableMessage(boolean request, boolean response) {
      this.request = request;
      this.response = response;
    }

    @Override
    public boolean request() {
      return request;
    }

    @Override
    public boolean response() {
      return response;
    }
  }
}
