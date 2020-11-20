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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DockerCgroupsReaderTest {

  @Test
  void testCgroupFileMissing() {
    CgroupsReader cgroups = new CgroupsReader("doesnotexists");
    Assertions.assertEquals("", cgroups.readContainerId());
  }

  @Test
  void readContainerId(@TempDir File tempFolder) throws IOException {
    File file = new File(tempFolder, "cgroup");
    String expected = "987a1920640799b5bf5a39bd94e489e5159a88677d96ca822ce7c433ff350163";
    String content = "dummy\n11:devices:/ecs/bbc36dd0-5ee0-4007-ba96-c590e0b278d2/" + expected;
    Files.write(content.getBytes(Charsets.UTF_8), file);

    CgroupsReader cgroupsReader = new CgroupsReader(file.getPath());
    Assertions.assertEquals(expected, cgroupsReader.readContainerId());
  }
}
