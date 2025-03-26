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

package io.opentelemetry.javaagent.instrumentation.hypertrace.grpc.v1_6;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.Descriptors.Descriptor;
import io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.Descriptors.FileDescriptor;
import io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.DynamicMessage;
import io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.util.JsonFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtobufMessageConverter {
  private static final Logger log = LoggerFactory.getLogger(ProtobufMessageConverter.class);
  private static final Map<String, FileDescriptor> fileDescriptorCache = new HashMap<>();

  /**
   * Converts an unrelocated protobuf message into a relocated DynamicMessage via a byte-array
   * round-trip.
   *
   * @param message The original protobuf message (an instance of com.google.protobuf.Message).
   * @return A relocated DynamicMessage built from your relocated protobuf classes.
   * @throws Exception if conversion fails.
   */
  public static DynamicMessage convertToRelocatedDynamicMessage(Message message) throws Exception {
    // 1. Serialize the original message to bytes.
    byte[] messageBytes = message.toByteArray();

    // 2. Obtain the original (unrelocated) message descriptor.
    Descriptors.Descriptor originalDescriptor = message.getDescriptorForType();

    // 3. Build the relocated descriptor with all dependencies
    Descriptor relocatedDescriptor = getRelocatedDescriptor(originalDescriptor);
    if (relocatedDescriptor == null) {
      throw new IllegalStateException(
          "Could not find relocated descriptor for message type: "
              + originalDescriptor.getFullName());
    }

    // 4. Parse the original message bytes using the relocated descriptor.
    try {
      return DynamicMessage.parseFrom(relocatedDescriptor, messageBytes);
    } catch (Exception e) {
      log.debug("Failed to parse message bytes using relocated descriptor: {}", e.getMessage());
      throw e;
    }
  }

  /** Recursively builds relocated file descriptors with all dependencies. */
  private static Descriptor getRelocatedDescriptor(Descriptors.Descriptor originalDescriptor)
      throws Exception {
    Descriptors.FileDescriptor unrelocatedFileDescriptor = originalDescriptor.getFile();

    // Check if we've already processed this file descriptor
    String fileKey = unrelocatedFileDescriptor.getName();
    if (fileDescriptorCache.containsKey(fileKey)) {
      FileDescriptor relocatedFileDescriptor = fileDescriptorCache.get(fileKey);
      return relocatedFileDescriptor.findMessageTypeByName(originalDescriptor.getName());
    }

    // Process all dependencies first
    List<FileDescriptor> dependencies = new ArrayList<>();
    for (Descriptors.FileDescriptor dependency : unrelocatedFileDescriptor.getDependencies()) {
      String depKey = dependency.getName();
      if (!fileDescriptorCache.containsKey(depKey)) {
        // Convert the dependency file descriptor
        com.google.protobuf.DescriptorProtos.FileDescriptorProto depProto = dependency.toProto();
        byte[] depBytes = depProto.toByteArray();
        FileDescriptorProto relocatedDepProto = FileDescriptorProto.parseFrom(depBytes);

        // Build with empty dependencies first (we'll fill them in later)
        FileDescriptor relocatedDep =
            FileDescriptor.buildFrom(relocatedDepProto, new FileDescriptor[] {});
        fileDescriptorCache.put(depKey, relocatedDep);
      }
      dependencies.add(fileDescriptorCache.get(depKey));
    }

    // Now build the current file descriptor with its dependencies
    com.google.protobuf.DescriptorProtos.FileDescriptorProto unrelocatedFileProto =
        unrelocatedFileDescriptor.toProto();
    byte[] fileProtoBytes = unrelocatedFileProto.toByteArray();
    FileDescriptorProto relocatedFileProto = FileDescriptorProto.parseFrom(fileProtoBytes);

    FileDescriptor relocatedFileDescriptor =
        FileDescriptor.buildFrom(relocatedFileProto, dependencies.toArray(new FileDescriptor[0]));
    fileDescriptorCache.put(fileKey, relocatedFileDescriptor);

    return relocatedFileDescriptor.findMessageTypeByName(originalDescriptor.getName());
  }

  /**
   * Method that takes an incoming message, converts it to a relocated one, prints it as JSON using
   * the relocated JsonFormat
   *
   * @param message The incoming (unrelocated) protobuf message.
   */
  public static String getMessage(Message message) {
    if (message == null) {
      log.debug("Cannot convert null message to JSON");
      return "";
    }

    try {
      // Convert the unrelocated message into a relocated DynamicMessage.
      DynamicMessage relocatedMessage = convertToRelocatedDynamicMessage(message);

      // Use the relocated JsonFormat to print the message as JSON.
      JsonFormat.Printer relocatedPrinter =
          JsonFormat.printer().includingDefaultValueFields().preservingProtoFieldNames();
      return relocatedPrinter.print(relocatedMessage);
    } catch (Exception e) {
      log.error("Failed to convert message to JSON: {}", e.getMessage(), e);
      if (log.isDebugEnabled()) {
        log.debug("Message type: {}", message.getClass().getName());
        log.debug("Message descriptor: {}", message.getDescriptorForType().getFullName());
      }
    }
    return "";
  }
}
