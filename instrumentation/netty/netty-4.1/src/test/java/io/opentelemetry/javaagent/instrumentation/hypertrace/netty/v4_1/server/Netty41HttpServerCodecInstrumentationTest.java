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

package io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1.server;

import io.netty.handler.codec.http.HttpServerCodec;
import java.util.Collections;

public class Netty41HttpServerCodecInstrumentationTest
    extends AbstractNetty41ServerInstrumentationTest {

  @Override
  protected NettyTestServer createNetty() {
    return new NettyTestServer(Collections.singletonList(HttpServerCodec.class));
  }
}
