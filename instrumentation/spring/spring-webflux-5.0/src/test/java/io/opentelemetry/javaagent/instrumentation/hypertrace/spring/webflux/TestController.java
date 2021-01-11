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

package io.opentelemetry.javaagent.instrumentation.hypertrace.spring.webflux;

import java.time.Duration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
class TestController {

  @GetMapping("/foo")
  Mono<FooModel> getFooModel() {
    return Mono.just(new FooModel("0", "DEFAULT"));
  }

  @GetMapping("/foo/{id}")
  Mono<FooModel> getFooModel(@PathVariable("id") String id) {
    return Mono.just(new FooModel(id, "pass"));
  }

  @GetMapping("/foo/{id}/{name}")
  Mono<FooModel> getFooModel(@PathVariable("id") String id, @PathVariable("name") String name) {
    return Mono.just(new FooModel(id, name));
  }

  @GetMapping("/foo-delayed")
  Mono<FooModel> getFooDelayed() {
    return Mono.just(new FooModel("3", "delayed")).delayElement(Duration.ofMillis(100));
  }

  @GetMapping("/foo-failfast/{id}")
  Mono<FooModel> getFooFailFast(@PathVariable("id") long id) {
    throw new RuntimeException("bad things happen");
  }

  @GetMapping("/foo-failmono/{id}")
  Mono<FooModel> getFooFailMono(@PathVariable("id") long id) {
    return Mono.error(new RuntimeException("bad things happen"));
  }

  @GetMapping("/foo-traced-method/{id}")
  Mono<FooModel> getTracedMethod(@PathVariable("id") String id) {
    return Mono.just(tracedMethod(id));
  }

  @GetMapping("/foo-mono-from-callable/{id}")
  Mono<FooModel> getMonoFromCallable(@PathVariable("id") String id) {
    return Mono.fromCallable(() -> tracedMethod(id));
  }

  @GetMapping("/foo-delayed-mono/{id}")
  Mono<FooModel> getFooDelayedMono(@PathVariable("id") long id) {
    return Mono.just(id)
        .delayElement(Duration.ofMillis(100))
        .map(i -> tracedMethod(String.valueOf(i)));
  }

  private FooModel tracedMethod(String id) {
    return new FooModel(id, "tracedMethod");
  }
}
