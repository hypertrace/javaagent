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

package io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_0.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.netty.v4.common.HttpRequestAndChannel;
import java.util.concurrent.ConcurrentHashMap;
import io.opentelemetry.javaagent.instrumentation.netty.v4_0.AttributeKeys;
import io.opentelemetry.javaagent.instrumentation.netty.v4_0.client.HttpClientRequestTracingHandler;

/**
 * Custom extension of OpenTelemetry's HttpClientRequestTracingHandler that ensures proper context
 * propagation by using Context.current() as the parent context.
 */
public class OtelHttpClientRequestTracingHandler extends HttpClientRequestTracingHandler {

    // Store the server context for each thread
    private static final ThreadLocal<Context> SERVER_CONTEXT = new ThreadLocal<>();

    // Store the mapping from thread ID to server span context (for cross-thread scenarios)
    private static final ConcurrentHashMap<Long, SpanContext> THREAD_TO_SPAN_CONTEXT =
            new ConcurrentHashMap<>();

    // Maximum size for the thread map before triggering cleanup
    private static final int MAX_THREAD_MAP_SIZE = 1000;

    // Cleanup flag to avoid excessive synchronized blocks
    private static volatile boolean cleanupNeeded = false;

    public OtelHttpClientRequestTracingHandler() {
        super();
    }

    /**
     * Stores the current context as the server context for this thread. This should be called from
     * the server handler.
     */
    public static void storeServerContext(Context context) {
        SERVER_CONTEXT.set(context);

        // Also store the span context by thread ID for cross-thread lookup
        Span span = Span.fromContext(context);
        if (span != null && span.getSpanContext().isValid()) {
            THREAD_TO_SPAN_CONTEXT.put(Thread.currentThread().getId(), span.getSpanContext());

            // Check if we need to clean up the map
            if (THREAD_TO_SPAN_CONTEXT.size() > MAX_THREAD_MAP_SIZE) {
                cleanupNeeded = true;
            }
        }
    }

    /**
     * Perform cleanup of the thread map if it has grown too large. This is done in a synchronized
     * block to prevent concurrent modification issues.
     */
    private static void cleanupThreadMapIfNeeded() {
        if (cleanupNeeded) {
            synchronized (THREAD_TO_SPAN_CONTEXT) {
                if (THREAD_TO_SPAN_CONTEXT.size() > MAX_THREAD_MAP_SIZE) {
                    THREAD_TO_SPAN_CONTEXT.clear();
                    cleanupNeeded = false;
                }
            }
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise prm) {
        try {
            if (!(msg instanceof HttpRequest)) {
                super.write(ctx, msg, prm);
                return;
            }

            Context parentContext = SERVER_CONTEXT.get();

            // Fallback -> If no context in thread local, try Context.current()
            if (parentContext == null) {
                parentContext = Context.current();
            }

            // Store the parent context in the channel attributes
            // This is used by the Opentelemetry's HttpClientRequestTracingHandler in propagating correct
            // context.
            ctx.channel().attr(AttributeKeys.CLIENT_PARENT_CONTEXT).set(parentContext);

            // Call the parent implementation which will use our stored parent context
            super.write(ctx, msg, prm);

            // Clean up after use to prevent memory leaks
            SERVER_CONTEXT.remove();
            THREAD_TO_SPAN_CONTEXT.remove(Thread.currentThread().getId());
            cleanupThreadMapIfNeeded();

        } catch (Exception ignored) {
        }
    }
}
