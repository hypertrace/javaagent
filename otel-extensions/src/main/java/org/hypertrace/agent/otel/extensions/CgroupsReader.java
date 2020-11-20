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

package org.hypertrace.agent.otel.extensions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CgroupsReader {

  private static final Logger log =
      LoggerFactory.getLogger(HypertraceResourceProvider.class.getName());

  private static final String DEFAULT_CGROUPS_PATH = "/proc/self/cgroup";
  private static final int CONTAINER_ID_LENGTH = 64;

  private final String cgroupsPath;

  CgroupsReader() {
    this.cgroupsPath = DEFAULT_CGROUPS_PATH;
  }

  CgroupsReader(String cgroupsPath) {
    this.cgroupsPath = cgroupsPath;
  }

  /**
   * Read container ID from cgroups file.
   *
   * @return docker container ID or empty string if not defined.
   */
  @SuppressWarnings("DefaultCharset")
  public String readContainerId() {
    try (BufferedReader br = new BufferedReader(new FileReader(cgroupsPath))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.length() > CONTAINER_ID_LENGTH) {
          return line.substring(line.length() - CONTAINER_ID_LENGTH);
        }
      }
    } catch (FileNotFoundException ex) {
      log.warn("Failed to read container id, cgroup file does not exist.", ex);
    } catch (IOException ex) {
      log.warn("Unable to read container id", ex);
    }
    return "";
  }
}
