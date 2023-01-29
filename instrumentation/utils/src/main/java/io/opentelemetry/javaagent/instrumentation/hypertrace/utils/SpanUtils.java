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

package io.opentelemetry.javaagent.instrumentation.hypertrace.utils;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Contains static utility methods for Spans. */
public abstract class SpanUtils {

  static final String HTTP_URL_ATTRIBUTE_KEY = "http.url";
  private static final Logger logger = LoggerFactory.getLogger(SpanUtils.class);

  /**
   * A List of BiConsumer implementations, each of which takes an HttpServletRequest and creates an
   * attribute that may be missing from the Span.
   */
  private static final List<BiConsumer<HttpServletRequest, Map<String, String>>>
      MISSING_ATTRIBUTE_PROVIDERS_WITH_REQUEST = initMissingAttributeProvidersWithRequest();

  /**
   * A List of BiConsumer implementations, each of which takes a URI and creates an attribute that
   * may be missing from the Span.
   */
  private static final List<BiConsumer<String, Map<String, String>>>
      MISSING_ATTRIBUTE_PROVIDERS_WITH_URI = initMissingAttributeProvidersWithURI();

  private static List<BiConsumer<HttpServletRequest, Map<String, String>>>
      initMissingAttributeProvidersWithRequest() {
    List<BiConsumer<HttpServletRequest, Map<String, String>>> returnList = new ArrayList<>();

    returnList.add(SpanUtils::addHttpURLAttributeToSpanFromRequest);
    return returnList;
  }

  private static List<BiConsumer<String, Map<String, String>>>
      initMissingAttributeProvidersWithURI() {
    List<BiConsumer<String, Map<String, String>>> returnList = new ArrayList<>();

    returnList.add(SpanUtils::addHttpURLAttributeToSpanFromURI);
    return returnList;
  }

  /**
   * Returns a Map of attributes, each of which represents an attribute that may be missing from the
   * Span.
   *
   * <p>Note that the Span itself is not an argument: the Span attributes cannot be accessed to
   * determine if the attributes being built here are required. They are built unconditionally; they
   * must be compared to the Span attributes at a later stage, when the existing Span attributes can
   * be accessed.
   *
   * @param servletRequest The HttpServletRequest, which is used to build the (possibly) missing
   *     attributes.
   * @return
   */
  public static Map<String, String> getPossiblyMissingSpanAttributes(
      HttpServletRequest servletRequest) {
    Map<String, String> returnMap = new HashMap<>();

    for (BiConsumer<HttpServletRequest, Map<String, String>> nextMissingAttrConsumer :
        MISSING_ATTRIBUTE_PROVIDERS_WITH_REQUEST) {
      nextMissingAttrConsumer.accept(servletRequest, returnMap);
    }

    return returnMap;
  }

  /**
   * Returns a Map of attributes, each of which represents an attribute that may be missing from the
   * Span.
   *
   * <p>Note that the Span itself is not an argument: the Span attributes cannot be accessed to
   * determine if the attributes being built here are required. They are built unconditionally; they
   * must be compared to the Span attributes at a later stage, when the existing Span attributes can
   * be accessed.
   *
   * @param uri A String containing the URI, which is used to build the (possibly) missing
   *     attributes.
   * @return
   */
  public static Map<String, String> getPossiblyMissingSpanAttributesFromURI(String uri) {
    Map<String, String> returnMap = new HashMap<>();
    for (BiConsumer<String, Map<String, String>> nextMissingAttrConsumer :
        MISSING_ATTRIBUTE_PROVIDERS_WITH_URI) {
      nextMissingAttrConsumer.accept(uri, returnMap);
    }

    return returnMap;
  }

  /**
   * Builds an "http.url" attribute from the HttpServletRequest
   *
   * @param servletRequest
   * @param newAttrMap
   */
  private static void addHttpURLAttributeToSpanFromRequest(
      HttpServletRequest servletRequest, Map<String, String> newAttrMap) {

    newAttrMap.put(HTTP_URL_ATTRIBUTE_KEY, servletRequest.getRequestURL().toString());
  }

  /**
   * Builds an "http.url" attribute from the URI
   *
   * @param uriAsString
   * @param newAttrMap
   */
  private static void addHttpURLAttributeToSpanFromURI(
      String uriAsString, Map<String, String> newAttrMap) {
    try {
      URI uri = new URI(uriAsString);
      URL url = uri.toURL();
      newAttrMap.put(HTTP_URL_ATTRIBUTE_KEY, url.toString());
    } catch (Exception e) {
      logger.error("Unable to add URL to Span: {}", e.toString());
    }
  }
}
