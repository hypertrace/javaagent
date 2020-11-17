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

package org.hypertrace.agent.filter.opa.custom;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.Span;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.hypertrace.agent.filter.api.ExecutionBlocked;
import org.hypertrace.agent.filter.api.ExecutionNotBlocked;
import org.hypertrace.agent.filter.api.Filter;
import org.hypertrace.agent.filter.api.FilterResult;
import org.hypertrace.agent.filter.opa.custom.evaluator.ICustomPolicyEvaluator;
import org.hypertrace.agent.filter.opa.custom.evaluator.IpAddressPolicyEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is a legacy code ported from Traceable Java agent. */
@AutoService(Filter.class)
public class CustomOpaLib implements Filter {
  private static final Logger log = LoggerFactory.getLogger(CustomOpaLib.class);

  private final OpaCommunicator opaCommunicator = new OpaCommunicator();
  private final Set<ICustomPolicyEvaluator> policyEvaluators = new HashSet<>();

  private final ScheduledExecutorService scheduledExecutorService =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactory() {
            private final AtomicInteger threadSequence = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
              String name = "hypertrace-agent-custom-opa" + threadSequence.getAndIncrement();
              Thread thread = new Thread(r, name);
              thread.setDaemon(true);
              return thread;
            }
          });

  public CustomOpaLib(String endpoint, String apiKey, boolean skipVerify, int maxDelay) {
    opaCommunicator.init(endpoint, apiKey, skipVerify);
    scheduledExecutorService.scheduleWithFixedDelay(
        new Runnable() {
          @Override
          public void run() {
            try {
              opaCommunicator.pollBlockingData();
            } catch (Throwable t) {
              log.debug("Unable to poll blocking data", t);
            }
          }
        },
        0,
        maxDelay,
        TimeUnit.SECONDS);

    policyEvaluators.add(new IpAddressPolicyEvaluator());
  }

  // TODO agent should clear resources at the end
  //  @Override
  //  public void fini() {
  //    scheduledExecutorService.shutdownNow();
  //    scheduledExecutorService = null;
  //    opaCommunicator.clear();
  //    policyEvaluators.clear();
  //  }

  @Override
  public FilterResult evaluateRequestHeaders(Span span, Map<String, String> headers) {
    // currently as per policy.rego, allowed list has precedence over denylist
    boolean allow =
        policyEvaluators.stream()
            .map(
                policyEvaluator ->
                    policyEvaluator.allow(opaCommunicator.getBlockingData(), headers))
            .anyMatch(Boolean::booleanValue);
    return allow ? ExecutionNotBlocked.INSTANCE : ExecutionBlocked.INSTANCE;
  }

  @Override
  public FilterResult evaluateRequestBody(Span span, String body) {
    return ExecutionNotBlocked.INSTANCE;
  }
}
