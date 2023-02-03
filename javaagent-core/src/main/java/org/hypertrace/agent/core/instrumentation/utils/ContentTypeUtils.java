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

package org.hypertrace.agent.core.instrumentation.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hypertrace.agent.core.config.DataCaptureConfig;

public class ContentTypeUtils {
  private final String[] collectableContentTypes;

  private static final String[] DEFAULT_CONTENT_TYPES =
      new String[] {"json", "graphql", "xml", "x-www-form-urlencoded"};

  public static String[] getDefaultContentTypes() {
    return DEFAULT_CONTENT_TYPES;
  }

  /** Loads the set of collectable content types from the DataCaptureConfig. */
  private ContentTypeUtils() {
    super();

    DataCaptureConfig dataCaptureConfig = DataCaptureConfig.ConfigProvider.get();

    if (dataCaptureConfig == null) {
      collectableContentTypes = DEFAULT_CONTENT_TYPES;
    } else {
      collectableContentTypes = dataCaptureConfig.getAllowedContentTypes();
    }
  }

  private static final String CHARSET_EQUALS = "charset=";
  private static final String SEPARATOR = ";";

  private static volatile ContentTypeUtils instance;

  private static ContentTypeUtils getInstance() {
    if (instance == null) {
      return getInstanceSync();
    }
    return instance;
  }

  private static synchronized ContentTypeUtils getInstanceSync() {
    if (instance == null) {
      instance = new ContentTypeUtils();
    }

    return instance;
  }

  /**
   * @param contentType content type to be checked
   * @return true if the content type should be captured by the agent.
   */
  private boolean shouldCapture_(String contentType) {
    if (contentType == null) {
      return false;
    }
    contentType = contentType.toLowerCase();

    for (String nextValidContentType : collectableContentTypes) {
      if (contentType.contains(nextValidContentType)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns true if the request/response with this content type should be captured.
   *
   * @param contentType request or response content type
   * @return whether body with this content type should be captured or not
   */
  public static boolean shouldCapture(String contentType) {
    return getInstance().shouldCapture_(contentType);
  }

  public static String parseCharset(String contentType) {
    if (contentType == null) {
      return null;
    }

    contentType = contentType.toLowerCase();
    int indexOfCharset = contentType.indexOf(CHARSET_EQUALS);
    if (indexOfCharset == -1) {
      return null;
    }

    int indexOfEncoding = indexOfCharset + CHARSET_EQUALS.length();
    if (indexOfEncoding < contentType.length()) {
      String substring = contentType.substring(indexOfEncoding, contentType.length());
      int semicolonIndex = substring.indexOf(SEPARATOR);
      if (semicolonIndex == -1) {
        return substring;
      } else {
        return substring.substring(0, semicolonIndex);
      }
    }
    return null;
  }

  /**
   * Converts an arbitrary object into JSON format.
   *
   * @param obj an arbitrary object
   * @return a string representation of the obj in JSON format
   */
  public static String convertToJSONString(Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
