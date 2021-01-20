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

package io.opentelemetry.javaagent.instrumentation.hypertrace.servlet.rw.reader;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import org.hypertrace.agent.testing.AbstractInstrumenterTest;
import org.junit.jupiter.api.Test;

public class BufferedReaderInstrumentationTest extends AbstractInstrumenterTest {

  @Test
  public void read() throws IOException {
    CharArrayReader arrayReader = new CharArrayReader("hello".toCharArray());
    //    BufferedReader bufferedReader = new TestBufferedReader(arrayReader);
    BufferedReader bufferedReader = new BufferedReader(arrayReader);
    System.out.println(bufferedReader.getClass().getName());
    bufferedReader.read();
    bufferedReader.read(new char[1]);
    bufferedReader.readLine();
  }
}
