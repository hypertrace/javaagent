package org.hypertrace.agent.otel.extensions.sampler;

class GlobToRegex {
  private GlobToRegex() {}

  public static String convertGlobToRegex(String pattern) {
    StringBuilder sb = new StringBuilder(pattern.length());
    for (int i = 0; i < pattern.length(); i++) {
      char ch = pattern.charAt(i);
      if (ch == '*') {
        if (i + 1 < pattern.length() && pattern.charAt(i+1) == '*') {
          sb.append(".*");
        } else {
          sb.append("[^\\/]*(|\\/)+");
        }
      } else if (ch == '*' && i > 0 && pattern.charAt(i-1) == '*') {
        continue;
      }  else{
        sb.append(ch);
      }
    }
    return sb.toString();
  }
}
