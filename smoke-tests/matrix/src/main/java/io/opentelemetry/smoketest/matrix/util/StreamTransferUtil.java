package io.opentelemetry.smoketest.matrix.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public final class StreamTransferUtil {
  // We have to run on Java 8, so no Java 9 stream transfer goodies for us.
  public static long transfer(InputStream from, OutputStream to) throws IOException {
    Objects.requireNonNull(to, "out");
    long transferred = 0;
    byte[] buffer = new byte[65535];
    int read;
    while ((read = from.read(buffer, 0, buffer.length)) >= 0) {
      to.write(buffer, 0, read);
      transferred += read;
    }
    return transferred;
  }
}
