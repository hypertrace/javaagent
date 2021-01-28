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

package io.opentelemetry.javaagent.instrumentation.hypertrace.struts;

import com.opensymphony.xwork2.ActionSupport;
import java.io.IOException;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;

@Result(type = "json")
@ParentPackage("json-default")
public class Struts2Action extends ActionSupport {

  private String jsonString;

  static final String sample = "{'balance':1000.21,'is_vip':true,'num':100,'name':'foo'}";

  public String body() throws IOException {
    jsonString = sample;
    return "body";
  }

  public String headers() {
    return "headers";
  }

  public String getJsonString() {
    return jsonString;
  }

  public void setJsonString(String jsonString) {
    this.jsonString = jsonString;
  }
}
