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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors;
import io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.Descriptors.Descriptor;
import io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.Descriptors.FileDescriptor;
import io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import io.opentelemetry.javaagent.instrumentation.hypertrace.com.google.protobuf.util.JsonFormat;

public class ProtobufRoundTripConverter {

  /**
   * Converts an unrelocated protobuf message into a relocated DynamicMessage via a byte-array
   * round-trip.
   *
   * @param message The original protobuf message (an instance of com.google.protobuf.Message).
   * @return A relocated DynamicMessage built from your relocated protobuf classes.
   * @throws Exception if conversion fails.
   */
  public static DynamicMessage convertToRelocatedDynamicMessage(Object message) throws Exception {
    if (!(message instanceof Message)) {
      throw new IllegalArgumentException("message is not a protobuf Message");
    }
    // 1. Serialize the original message to bytes.
    Message originalMessage = (Message) message;
    byte[] messageBytes = originalMessage.toByteArray();

    // 2. Obtain the original (unrelocated) message descriptor.
    Descriptors.Descriptor originalDescriptor = originalMessage.getDescriptorForType();

    // 3. Get the unrelocated file descriptor and its proto representation.
    Descriptors.FileDescriptor unrelocatedFileDescriptor = originalDescriptor.getFile();
    com.google.protobuf.DescriptorProtos.FileDescriptorProto unrelocatedFileProto =
            unrelocatedFileDescriptor.toProto();
    byte[] fileProtoBytes = unrelocatedFileProto.toByteArray();

    // 4. Parse the file descriptor proto using relocated classes.
    // This converts the unrelocated FileDescriptorProto into your relocated FileDescriptorProto.
    FileDescriptorProto relocatedFileProto = FileDescriptorProto.parseFrom(fileProtoBytes);

    // 5. Build the relocated FileDescriptor.
    // Note: This example assumes there are no dependencies.
    FileDescriptor relocatedFileDescriptor =
        FileDescriptor.buildFrom(relocatedFileProto, new FileDescriptor[] {});

    // 6. Find the relocated message descriptor by name.
    // We assume the message name is the same in both relocated and unrelocated files.
    Descriptor relocatedDescriptor =
        relocatedFileDescriptor.findMessageTypeByName(originalDescriptor.getName());
    if (relocatedDescriptor == null) {
      throw new IllegalStateException(
          "Could not find relocated descriptor for message type: " + originalDescriptor.getName());
    }

    // 7. Parse the original message bytes using the relocated descriptor.
    DynamicMessage relocatedMessage = DynamicMessage.parseFrom(relocatedDescriptor, messageBytes);
    return relocatedMessage;
  }

  /**
   * Example method that takes an incoming message, converts it to a relocated one, prints it as
   * JSON using the relocated JsonFormat, and attaches it as a span attribute.
   *
   * @param message The incoming (unrelocated) protobuf message.
   * @param span The span to which to attach the attribute.
   * @param key The attribute key.
   */
  public static void addConvertedMessageAttribute(
      Object message, Span span, AttributeKey<String> key) {
    try {
      // Convert the unrelocated message into a relocated DynamicMessage.
      DynamicMessage relocatedMessage = convertToRelocatedDynamicMessage(message);

      // Use the relocated JsonFormat to print the message as JSON.
      JsonFormat.Printer relocatedPrinter = JsonFormat.printer();
      String jsonOutput = relocatedPrinter.print(relocatedMessage);

      // Set the JSON output as a span attribute.
      span.setAttribute(key, jsonOutput);
    } catch (Exception e) {
      System.err.println("Failed to convert message via byte-array round-trip: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
