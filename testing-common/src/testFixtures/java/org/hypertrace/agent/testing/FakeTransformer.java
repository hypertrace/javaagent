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

package org.hypertrace.agent.testing;

import io.opentelemetry.javaagent.bootstrap.ClassFileTransformerHolder;
import java.io.*;
import java.lang.instrument.ClassFileTransformer;

/**
 * This is a utility class which can be called within a unit test. It's purpose is to re-invoke the
 * OTEL class transformer for a class, and then write the result class bytes to an external file,
 * where it can be examined.
 */
public class FakeTransformer {

  private final ClassLoader classLoader;
  private ClassFileTransformer transformer;

  public FakeTransformer() {
    super();
    classLoader = FakeTransformer.class.getClassLoader();
    transformer = ClassFileTransformerHolder.getClassFileTransformer();
  }

  /**
   * Re-transforms a class, and then writes the before and after images to a file system directory.
   *
   * @param className
   * @param targetDir
   */
  public void writeTransformedClass(String className, String targetDir) {
    BufferedInputStream bis = null;
    ByteArrayOutputStream baos = null;

    String resourceName = className.replace('.', '/') + ".class";

    try {
      InputStream is = classLoader.getResourceAsStream(resourceName);

      try {
        baos = new ByteArrayOutputStream();
        bis = new BufferedInputStream(is);

        copy(bis, baos);
      } finally {
        if (baos != null) {
          baos.close();
        }

        if (bis != null) {
          bis.close();
        }
      }

      File beforeFile = new File(new File(targetDir), "before" + File.separator + resourceName);

      beforeFile.getParentFile().mkdirs();

      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(beforeFile));
      bis = new BufferedInputStream(new ByteArrayInputStream(baos.toByteArray()));

      try {
        copy(bis, bos);
      } finally {
        if (bos != null) {
          bos.close();
        }

        if (bis != null) {
          bis.close();
        }
      }

      byte[] result = transformer.transform(classLoader, className, null, null, baos.toByteArray());

      if (result != null) {
        System.err.println("Received transformed bytes");

        File outFile = new File(new File(targetDir), "after" + File.separator + resourceName);

        outFile.getParentFile().mkdirs();

        bos = new BufferedOutputStream(new FileOutputStream(outFile));
        bis = new BufferedInputStream(new ByteArrayInputStream(result));

        try {
          copy(bis, bos);
        } finally {
          if (bos != null) {
            bos.close();
          }

          if (bis != null) {
            bis.close();
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }

  private static void copy(InputStream is, OutputStream os) throws IOException {

    int available;

    while ((available = is.available()) > 0) {
      byte[] buffer = new byte[available];

      int bytesRead = is.read(buffer);

      if (bytesRead < 0) {
        break;
      }

      if (bytesRead > 0) {
        os.write(buffer, 0, bytesRead);
      }
    }
  }
}
