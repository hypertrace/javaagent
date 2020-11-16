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

package org.hypertrace.agent.filter.opa.custom.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** data.json java class mapping for blocking policy. */
public class BlockingData {
  private IpAddress denylist;
  private List<IpAddressWithExpiry> suspendedlist;
  private IpAddress allowlist;
  private List<IpAddressWithExpiry> snoozedlist;

  public BlockingData() {}

  public BlockingData(
      IpAddress denylist,
      List<IpAddressWithExpiry> suspendedlist,
      IpAddress allowlist,
      List<IpAddressWithExpiry> snoozedlist) {
    this.denylist = denylist;
    this.suspendedlist = suspendedlist;
    this.allowlist = allowlist;
    this.snoozedlist = snoozedlist;
  }

  public static BlockingData createEmpty() {
    return new BlockingData(
        new IpAddress(new HashSet<>()),
        new ArrayList<>(),
        new IpAddress(new HashSet<>()),
        new ArrayList<>());
  }

  public IpAddress getDenylist() {
    return denylist;
  }

  public void setDenylist(IpAddress denylist) {
    this.denylist = denylist;
  }

  public List<IpAddressWithExpiry> getSuspendedlist() {
    return suspendedlist;
  }

  public void setSuspendedlist(List<IpAddressWithExpiry> suspendedlist) {
    this.suspendedlist = suspendedlist;
  }

  public IpAddress getAllowlist() {
    return allowlist;
  }

  public void setAllowlist(IpAddress allowlist) {
    this.allowlist = allowlist;
  }

  public List<IpAddressWithExpiry> getSnoozedlist() {
    return snoozedlist;
  }

  public void setSnoozedlist(List<IpAddressWithExpiry> snoozedlist) {
    this.snoozedlist = snoozedlist;
  }
}
