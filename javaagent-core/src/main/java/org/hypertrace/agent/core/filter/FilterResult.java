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

package org.hypertrace.agent.core.filter;

public class FilterResult {

  private final boolean shouldBlock;
  private final int blockingStatusCode;
  private final String blockingMsg;

  public FilterResult(boolean shouldBlock, int blockingStatusCode, String blockingMsg) {
    this.shouldBlock = shouldBlock;
    this.blockingStatusCode = blockingStatusCode;
    this.blockingMsg = blockingMsg;
  }

  public boolean shouldBlock() {
    return shouldBlock;
  }

  public int getBlockingStatusCode() {
    return blockingStatusCode;
  }

  public String getBlockingMsg() {
    return blockingMsg;
  }
}
