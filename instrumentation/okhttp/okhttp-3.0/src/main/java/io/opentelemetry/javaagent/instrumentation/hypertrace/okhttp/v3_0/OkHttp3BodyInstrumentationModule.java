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

package io.opentelemetry.javaagent.instrumentation.hypertrace.okhttp.v3_0;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Collections;
import java.util.List;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

@AutoService(InstrumentationModule.class)
public class OkHttp3BodyInstrumentationModule extends InstrumentationModule {

  public OkHttp3BodyInstrumentationModule() {
    super(Okhttp3InstrumentationName.PRIMARY, Okhttp3InstrumentationName.OTHER);
  }

  @Override
  public int order() {
    return 1;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new OkHttp3BodyInstrumentation());
  }

  private static final class OkHttp3BodyInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return named("okhttp3.OkHttpClient");
    }

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
          isConstructor().and(takesArgument(0, named("okhttp3.OkHttpClient$Builder"))),
          OkHttp3BodyInstrumentationModule.class.getName() + "$OkHttp3Advice");
    }
  }

  public static class OkHttp3Advice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void addTracingInterceptor(@Advice.Argument(0) OkHttpClient.Builder builder) {
      for (Interceptor interceptor : builder.interceptors()) {
        if (interceptor instanceof OkHttpTracingInterceptor) {
          return;
        }
      }
      OkHttpTracingInterceptor interceptor = new OkHttpTracingInterceptor();
      builder.addNetworkInterceptor(interceptor);
    }
  }
}
