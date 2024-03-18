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

package org.hypertrace.agent.testing;

import com.google.common.base.Stopwatch;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.proto.collector.trace.v1.ExportTracePartialSuccess;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;

public class TestOtlpReceiver implements AutoCloseable {

  private final Server server = new Server(4318);
  private final HandlerList handlerList = new HandlerList();

  private static final List<List<Span>> traces = new ArrayList<>(); // guarded by tracesLock
  private static final Object tracesLock = new Object();

  public void start() throws Exception {
    HandlerList handlerList = new HandlerList();
    server.stop();
    handlerList.addHandler(new OtlpTracesHandler());
    server.setHandler(handlerList);
    server.start();
  }

  public void addHandler(Handler handler) {
    this.handlerList.addHandler(handler);
  }

  @Override
  public void close() throws Exception {
    server.stop();
  }

  public int port() {
    return server.getConnectors()[0].getLocalPort();
  }

  static class OtlpTracesHandler extends AbstractHandler {
    @Override
    public void handle(
        String target,
        Request baseRequest,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {

      if (target.equals("/v1/traces")
          && ("post".equalsIgnoreCase(request.getMethod())
              || "put".equalsIgnoreCase(request.getMethod()))) {
        ServletInputStream inputStream = request.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        while ((nRead = inputStream.read()) != -1) {
          buffer.write((byte) nRead);
        }

        ExportTraceServiceRequest traceServiceRequest =
            ExportTraceServiceRequest.newBuilder().mergeFrom(buffer.toByteArray()).build();

        for (ResourceSpans resourceSpans : traceServiceRequest.getResourceSpansList()) {
          for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
            for (Span span : scopeSpans.getSpansList()) {
              synchronized (tracesLock) {
                boolean found = false;
                for (List<Span> trace : traces) {
                  if (trace.get(0).getTraceId().equals(span.getTraceId())) {
                    trace.add(span);
                    found = true;
                    break;
                  }
                }
                if (!found) {
                  List<Span> trace = new CopyOnWriteArrayList<>();
                  trace.add(span);
                  traces.add(trace);
                }
                tracesLock.notifyAll();
              }
            }
          }
        }

        response.setStatus(200);
        response.setContentType(request.getContentType());
        response
            .getWriter()
            .print(
                ExportTraceServiceResponse.newBuilder()
                    .setPartialSuccess(
                        ExportTracePartialSuccess.newBuilder().setRejectedSpans(0).build()));
        baseRequest.setHandled(true);
      }
    }
  }

  private List<List<Span>> getTraces() {
    synchronized (tracesLock) {
      // always return a copy so that future structural changes cannot cause race conditions during
      // test verification
      List<List<Span>> copy = new ArrayList<>(traces.size());
      for (List<Span> trace : traces) {
        copy.add(new ArrayList<>(trace));
      }
      return copy;
    }
  }

  public void waitForTraces(int number) throws InterruptedException, TimeoutException {
    waitForTraces(number, spans -> false);
  }

  public List<List<Span>> waitForTraces(int number, Predicate<List<Span>> excludes)
      throws InterruptedException, TimeoutException {
    synchronized (tracesLock) {
      long remainingWaitMillis = TimeUnit.SECONDS.toMillis(31);
      List<List<Span>> traces = getCompletedAndFilteredTraces(excludes, span -> false);
      while (traces.size() < number && remainingWaitMillis > 0) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        tracesLock.wait(remainingWaitMillis);
        remainingWaitMillis -= stopwatch.elapsed(TimeUnit.MILLISECONDS);
        traces = getCompletedAndFilteredTraces(excludes, span -> false);
      }
      if (traces.size() < number) {
        throw new TimeoutException(
            "Timeout waiting for "
                + number
                + " completed/filtered trace(s), found "
                + traces.size()
                + " completed/filtered trace(s) and "
                + traces.size()
                + " total trace(s): "
                + traces);
      }
      return traces;
    }
  }

  private int spansCount(List<List<Span>> traces) {
    int count = 0;
    for (List<Span> trace : traces) {
      count += trace.size();
    }
    return count;
  }

  public List<List<Span>> waitForSpans(int number) throws InterruptedException, TimeoutException {
    return waitForSpans(number, span -> false);
  }

  public List<List<Span>> waitForSpans(int number, Predicate<Span> excludes)
      throws InterruptedException, TimeoutException {
    synchronized (tracesLock) {
      long remainingWaitMillis = TimeUnit.SECONDS.toMillis(31);

      List<List<Span>> traces = getCompletedAndFilteredTraces(spans -> false, excludes);
      while (spansCount(traces) < number && remainingWaitMillis > 0) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        tracesLock.wait(remainingWaitMillis);
        remainingWaitMillis -= stopwatch.elapsed(TimeUnit.MILLISECONDS);
        traces = getCompletedAndFilteredTraces(spans -> false, excludes);
      }
      if (spansCount(traces) < number) {
        throw new TimeoutException(
            "Timeout waiting for "
                + number
                + " completed/filtered spans(s), found "
                + spansCount(traces)
                + " total spans(s): "
                + traces);
      }
      return traces;
    }
  }

  private List<Span> getFilteredSpans(List<Span> trace, Predicate<Span> excludeSpanPredicate) {
    List<Span> filteredSpans = new ArrayList<>();
    Predicate<Span> excludes =
        span -> {
          assert span != null;
          AnyValue target = getAttributesMap(span).get("http.target");
          boolean excludeSpan = false;
          if (target != null && target.getStringValue().contains("/v1/traces")) {
            excludeSpan = true;
          }
          AnyValue url = getAttributesMap(span).get("http.url");
          if (url != null && url.getStringValue().contains("/v1/traces")) {
            excludeSpan = true;
          }
          return excludeSpan;
        };
    for (Span span : trace) {
      if (!excludes.test(span) && !excludeSpanPredicate.test(span)) {
        filteredSpans.add(span);
      }
    }
    return filteredSpans;
  }

  private List<List<Span>> getCompletedAndFilteredTraces(
      Predicate<List<Span>> excludeTrace, Predicate<Span> excludeSpan) {
    List<List<Span>> traces = new ArrayList<>();
    for (List<Span> trace : getTraces()) {
      if (isCompleted(trace) && !excludeTrace.test(trace)) {
        List<Span> filteredTrace = getFilteredSpans(trace, excludeSpan);
        if (!filteredTrace.isEmpty()) {
          traces.add(filteredTrace);
        }
      }
    }
    return traces;
  }

  // trace is completed if root span is present
  private static boolean isCompleted(List<Span> trace) {
    for (Span span : trace) {
      if (!SpanId.isValid(span.getParentSpanId().toString())) {
        return true;
      }
    }
    return false;
  }

  public void clear() {
    synchronized (tracesLock) {
      traces.clear();
    }
  }

  public Map<String, AnyValue> getAttributesMap(Span span) {
    Map<String, AnyValue> attributesMap = new HashMap<>();
    for (KeyValue keyValue : span.getAttributesList()) {
      attributesMap.put(keyValue.getKey(), keyValue.getValue());
    }
    return attributesMap;
  }
}
