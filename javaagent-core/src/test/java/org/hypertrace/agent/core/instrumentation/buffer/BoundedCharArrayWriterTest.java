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

package org.hypertrace.agent.core.instrumentation.buffer;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BoundedCharArrayWriterTest {

  private static final String ONE_TO_TEN = "0123456789";

  @Test
  public void writeArrTheSameSizeAsBuffer() throws IOException {
    BoundedCharArrayWriter boundedBuffer = new BoundedCharArrayWriter(10);

    boundedBuffer.write(ONE_TO_TEN);
    Assertions.assertEquals(10, boundedBuffer.size());
    boundedBuffer.write("01234");
    Assertions.assertEquals(10, boundedBuffer.size());
    Assertions.assertEquals(ONE_TO_TEN, boundedBuffer.toString());
  }

  @Test
  public void writeArrSmallerSizeAsBuffer() throws IOException {
    BoundedCharArrayWriter boundedBuffer = new BoundedCharArrayWriter(15);

    boundedBuffer.write(ONE_TO_TEN);
    Assertions.assertEquals(10, boundedBuffer.size());
    boundedBuffer.write("0123456".toCharArray());
    Assertions.assertEquals(15, boundedBuffer.size());
    Assertions.assertEquals(ONE_TO_TEN + "01234", boundedBuffer.toString());
  }

  @Test
  public void writeByteSmallerSizeAsBuffer() throws IOException {
    BoundedCharArrayWriter boundedBuffer = new BoundedCharArrayWriter(6);

    boundedBuffer.write('0');
    boundedBuffer.write('1');
    boundedBuffer.write('2');
    boundedBuffer.write('3');
    boundedBuffer.write('4');
    Assertions.assertEquals(5, boundedBuffer.size());
    boundedBuffer.write('5');
    Assertions.assertEquals(6, boundedBuffer.size());
    boundedBuffer.write("012345".toCharArray());
    Assertions.assertEquals(6, boundedBuffer.size());
    Assertions.assertEquals("012345", boundedBuffer.toString());
  }
}
