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

package io.opentelemetry.javaagent.instrumentation.hypertrace.micronaut.v3;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Controller("/")
public class TestController {

  static final String RESPONSE_HEADER_NAME = "reqheadername";
  static final String RESPONSE_HEADER_VALUE = "reqheadervalue";
  static final String RESPONSE_BODY = "{\"key\": \"val\"}";

  @Get(uri = "/get_no_content")
  public HttpResponse<?> getNoContent() {
    return HttpResponse.status(HttpStatus.NO_CONTENT)
        .header(RESPONSE_HEADER_NAME, RESPONSE_HEADER_VALUE);
  }

  @Post(uri = "/post")
  public HttpResponse<?> post(@Body String body) {
    System.out.printf("Received body: %s", body);
    return HttpResponse.status(HttpStatus.OK)
        .header(RESPONSE_HEADER_NAME, RESPONSE_HEADER_VALUE)
        .characterEncoding(StandardCharsets.US_ASCII)
        .contentType(MediaType.APPLICATION_JSON)
        .body(RESPONSE_BODY);
  }

  @Get(uri = "/get_gzip")
  public HttpResponse<byte[]> getGzip() throws IOException {
    String jsonResponse = "{\"message\": \"hello\"}";

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteArrayOutputStream)) {
      gzipOut.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
      gzipOut.finish();
    }
    byte[] gzipData = byteArrayOutputStream.toByteArray();
    return HttpResponse.<byte[]>ok(gzipData)
        .contentType("application/json")
        .header("Content-Encoding", "gzip");
  }

  @Get(value = "/stream", produces = MediaType.APPLICATION_JSON_STREAM)
  public Flowable<String> stream() {
    return Flowable.fromIterable(streamBody());
  }

  static List<String> streamBody() {
    List<String> entities = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      entities.add(
          String.format(
              "dog,cats,foo,bar,dog,cats,foo,bar,dog,cats,foo,bar,dog,cats,foo,bar,dog,cats,foo,bar=%d\n",
              i));
    }
    return entities;
  }
}
