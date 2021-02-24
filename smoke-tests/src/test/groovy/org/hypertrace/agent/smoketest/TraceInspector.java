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

package org.hypertrace.agent.smoketest;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TraceInspector {
  final Collection<ExportTraceServiceRequest> traces;

  public TraceInspector(Collection<ExportTraceServiceRequest> traces) {
    this.traces = traces;
  }

  public Stream<Span> getSpanStream() {
    return traces.stream()
        .flatMap(it -> it.getResourceSpansList().stream())
        .flatMap(it -> it.getInstrumentationLibrarySpansList().stream())
        .flatMap(it -> it.getSpansList().stream());
  }

  public Stream<AnyValue> findResourceAttribute(String attributeKey) {
    return traces.stream()
        .flatMap(it -> it.getResourceSpansList().stream())
        .flatMap(it -> it.getResource().getAttributesList().stream())
        .filter(it -> it.getKey().equals(attributeKey))
        .map(KeyValue::getValue);
  }

  public long countFilteredResourceAttributes(String attributeName, Object attributeValue) {
    return traces.stream()
        .flatMap(it -> it.getResourceSpansList().stream())
        .map(ResourceSpans::getResource)
        .flatMap(it -> it.getAttributesList().stream())
        .filter(a -> a.getKey().equals(attributeName))
        .map(a -> a.getValue().getStringValue())
        .filter(s -> s.equals(attributeValue))
        .count();
  }

  public long countFilteredAttributes(String attributeName, Object attributeValue) {
    return getSpanStream()
        .flatMap(s -> s.getAttributesList().stream())
        .filter(a -> a.getKey().equals(attributeName))
        .map(a -> a.getValue().getStringValue())
        .filter(s -> s.equals(attributeValue))
        .count();
  }

  public long countFilteredAttributes(String attributeName) {
    return getSpanStream()
        .flatMap(s -> s.getAttributesList().stream())
        .filter(a -> a.getKey().equals(attributeName))
        .map(a -> a.getValue().getStringValue())
        .count();
  }

  public String getFilteredAttributeValue(String attributeName) {
    return getSpanStream()
        .flatMap(s -> s.getAttributesList().stream())
        .filter(a -> a.getKey().equals(attributeName))
        .map(a -> a.getValue().getStringValue())
        .collect(Collectors.joining(","));
  }

  public long countFilteredEventAttributes(String attributeName, Object attributeValue) {
    return getSpanStream()
        .flatMap(s -> s.getEventsList().stream())
        .flatMap(e -> e.getAttributesList().stream())
        .filter(a -> a.getKey().equals(attributeName))
        .map(a -> a.getValue().getStringValue())
        .filter(s -> s.equals(attributeValue))
        .count();
  }

  protected int countSpansByName(String spanName) {
    return (int) getSpanStream().filter(it -> it.getName().equals(spanName)).count();
  }

  protected int countSpansByKind(Span.SpanKind spanKind) {
    return (int) getSpanStream().filter(it -> it.getKind().equals(spanKind)).count();
  }

  protected int countSpans() {
    return (int) getSpanStream().count();
  }

  public int size() {
    return traces.size();
  }

  public Set<String> getTraceIds() {
    return getSpanStream()
        .map(Span::getTraceId)
        .map(ByteString::toByteArray)
        .map(SmokeTest::bytesToHex)
        .collect(Collectors.toSet());
  }
}
