/*
 * Copyright The Hypertrace Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.hypertrace.java.outputstream;

import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap;
import java.io.IOException;
import java.io.OutputStream;
import org.hypertrace.agent.core.GlobalObjectRegistry;

public class OutputStreamUtils {

  private OutputStreamUtils() {}

  public static boolean check(OutputStream outputStream) {
    Object outStream = GlobalObjectRegistry.outputStreamToBufferMap.get(outputStream);
    if (outStream == null) {
      return false;
    }

    int callDepth = CallDepthThreadLocalMap.incrementCallDepth(OutputStream.class);
    if (callDepth > 0) {
      return false;
    }
    return true;
  }

  public static void write(OutputStream thizzOutputStream, boolean retEnterAdvice, int b)
      throws IOException {
    if (!retEnterAdvice) {
      return;
    }

    Object outStream = GlobalObjectRegistry.outputStreamToBufferMap.get(thizzOutputStream);
    if (outStream == null) {
      return;
    }
    OutputStream outputStream = (OutputStream) outStream;
    outputStream.write(b);
    CallDepthThreadLocalMap.reset(OutputStream.class);
  }

  public static void write(OutputStream thizzOutputStream, boolean retEnterAdvice, byte[] b)
      throws IOException {
    if (!retEnterAdvice) {
      return;
    }

    Object outStream = GlobalObjectRegistry.outputStreamToBufferMap.get(thizzOutputStream);
    if (outStream == null) {
      return;
    }
    OutputStream outputStream = (OutputStream) outStream;
    outputStream.write(b);
    CallDepthThreadLocalMap.reset(OutputStream.class);
  }

  public static void write(
      OutputStream thizzOutputStream, boolean retEnterAdvice, byte[] b, int off, int len)
      throws IOException {
    if (!retEnterAdvice) {
      return;
    }
    Object outStream = GlobalObjectRegistry.outputStreamToBufferMap.get(thizzOutputStream);
    if (outStream == null) {
      return;
    }
    OutputStream outputStream = (OutputStream) outStream;
    outputStream.write(b, off, len);
    CallDepthThreadLocalMap.reset(OutputStream.class);
  }
}
