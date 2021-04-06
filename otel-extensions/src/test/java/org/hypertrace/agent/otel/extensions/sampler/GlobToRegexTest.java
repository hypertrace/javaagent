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

package org.hypertrace.agent.otel.extensions.sampler;

import static org.hypertrace.agent.otel.extensions.sampler.GlobToRegex.convertGlobToRegex;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GlobToRegexTest {

  @Test
  public void globbing() {
    Pattern pattern = Pattern.compile(convertGlobToRegex("/foo/**"));
    Assertions.assertEquals(false, pattern.matcher("/foe/bar/baz").matches());
    Assertions.assertEquals(true, pattern.matcher("/foo/bar/baz").matches());
    Assertions.assertEquals(true, pattern.matcher("/foo/bazar").matches());
  }

  @Test
  public void singleStar() {
    Pattern pattern = Pattern.compile(convertGlobToRegex("/foo/*"));
    Assertions.assertEquals(false, pattern.matcher("/foe/bar/baz").matches());
    Assertions.assertEquals(false, pattern.matcher("/foo/bar/baz").matches());
    Assertions.assertEquals(true, pattern.matcher("/foo/bar/").matches());
    Assertions.assertEquals(true, pattern.matcher("/foo/bazar").matches());
  }
}
