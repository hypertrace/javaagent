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

package org;

import java.io.BufferedReader;
import java.io.PrintWriter;
import org.hypertrace.agent.core.instrumentation.buffer.BoundedCharArrayWriter;
import org.hypertrace.agent.core.instrumentation.buffer.CharBufferSpanPair;

public class BufferedReaderPrintWriterContextAccess {

  private BufferedReaderPrintWriterContextAccess() {}

  public static void addToBufferedReaderContext(
      BufferedReader bufferedReader, CharBufferSpanPair buffer) {}

  public static void addToPrintWriterContext(
      PrintWriter printWriter, BoundedCharArrayWriter buffer) {}
}
