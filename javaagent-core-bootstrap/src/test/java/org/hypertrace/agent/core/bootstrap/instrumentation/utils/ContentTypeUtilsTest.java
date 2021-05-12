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

package org.hypertrace.agent.core.bootstrap.instrumentation.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ContentTypeUtilsTest {

  @Test
  public void shouldCapture() {
    String[] valid = {"jSoN", "graphQL", "x-www-forM-urlencoded"};
    for (String contentType : valid) {
      Assertions.assertTrue(ContentTypeUtils.shouldCapture(contentType), contentType);
    }
  }

  @Test
  public void shouldNotCapture() {
    String[] valid = {
      "text/plain",
    };
    for (String contentType : valid) {
      Assertions.assertFalse(ContentTypeUtils.shouldCapture(contentType), contentType);
    }
  }

  @Test
  public void utf8Charset() {
    Assertions.assertEquals(
        "utf-8", ContentTypeUtils.parseCharset("Content-Type: application/json; charset=utf-8"));
  }

  @Test
  public void windows1252Charset() {
    Assertions.assertEquals(
        "windows-1252",
        ContentTypeUtils.parseCharset("Content-Type: application/json; charset=windows-1252"));
  }

  @Test
  public void noCharset() {
    Assertions.assertEquals(null, ContentTypeUtils.parseCharset("Content-Type: application/json;"));
    Assertions.assertEquals(
        null, ContentTypeUtils.parseCharset("Content-Type: application/json; charset="));
  }
}
