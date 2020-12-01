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

package io.opentelemetry.instrumentation.hypertrace.grpc.v1_5;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.hypertrace.grpc.v1_5.client.GrpcClientBodyInstrumentation;
import io.opentelemetry.instrumentation.hypertrace.grpc.v1_5.server.GrpcServerBodyInstrumentation;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Arrays;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class GrpcBodyInstrumentationModule extends InstrumentationModule {

  public GrpcBodyInstrumentationModule() {
    super(GrpcInstrumentationName.PRIMARY, GrpcInstrumentationName.OTHER);
  }

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(new GrpcClientBodyInstrumentation(), new GrpcServerBodyInstrumentation());
  }
}
