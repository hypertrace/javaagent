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

package io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.v1_6;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.function.Supplier;

final class GrpcChannelProvider implements Supplier<ManagedChannel> {

  private final int port;

  GrpcChannelProvider(final int port) {
    this.port = port;
  }

  @Override
  public ManagedChannel get() {
    return ManagedChannelBuilder.forTarget(String.format("localhost:%d", port))
        .usePlaintext(true)
        .build();
  }
}
