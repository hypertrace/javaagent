package org;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

/**
 * OTEL agent excludes {@code io.opentelemetry.} package from instrumentation, therefore using this package.
 */
public class TestInputStream extends ServletInputStream {

  private final InputStream wrapped;

  public TestInputStream(InputStream wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public boolean isFinished() {
    return false;
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void setReadListener(ReadListener readListener) {

  }

  @Override
  public int read() throws IOException {
    return wrapped.read();
  }
}
