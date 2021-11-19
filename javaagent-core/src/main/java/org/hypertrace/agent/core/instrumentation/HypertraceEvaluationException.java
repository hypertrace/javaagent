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

package org.hypertrace.agent.core.instrumentation;

/**
 * A custom exception used to terminate the handling of a request thread after the initial
 * opportunity for middleware to look at the contents of an HTTP request and determine if it should
 * be processed before handing it off to application code for execution.
 *
 * <p>
 *
 * <p>Framework-specific instrumentation of middlewares (e.g. {@code javax.servlet.Filter}) or
 * implementations of framework-specific middlewares (e.g. {@code
 * io.netty.channel.ChannelInboundHandlerAdapter} and {@code io.grpc.ServerInterceptor}) should
 * handle the suppression of this exception in order to translate this runtime exception into a
 * framework-appropriate response (e.g. 403 response code)
 */
public final class HypertraceEvaluationException extends RuntimeException {

  private static final String DEFAULT_MESSAGE =
      "A filter implementation determined that this request should be blocked";

  public HypertraceEvaluationException() {
    super(DEFAULT_MESSAGE);
  }
}
