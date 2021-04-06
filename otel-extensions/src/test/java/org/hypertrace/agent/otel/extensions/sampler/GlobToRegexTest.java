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
