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

package org.hypertrace.agent.core.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.hypertrace.agent.config.ConfigurationServiceGrpc;
import org.hypertrace.agent.config.Service;

public class DynamicConfigServer {

  private final Server server;
  private Service.InitialConfigurationResponse initialConfigurationResponse;
  private Service.UpdateConfigurationResponse updateConfigurationResponse;

  public DynamicConfigServer(int port) {
    server = ServerBuilder.forPort(port).addService(new DynamicConfigService()).build();
  }

  /** Start serving requests. */
  public void start() throws IOException {
    server.start();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                  DynamicConfigServer.this.stop();
                } catch (InterruptedException e) {
                  e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
              }
            });
  }

  /** Stop serving requests and shutdown resources. */
  public void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /** Await termination on the main thread since the grpc library uses daemon threads. */
  public void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  public Service.InitialConfigurationResponse getInitialConfigurationResponse() {
    return initialConfigurationResponse;
  }

  public void setInitialConfigurationResponse(
      Service.InitialConfigurationResponse initialConfigurationResponse) {
    this.initialConfigurationResponse = initialConfigurationResponse;
  }

  public Service.UpdateConfigurationResponse getUpdateConfigurationResponse() {
    return updateConfigurationResponse;
  }

  public void setUpdateConfigurationResponse(
      Service.UpdateConfigurationResponse updateConfigurationResponse) {
    this.updateConfigurationResponse = updateConfigurationResponse;
  }

  /** Main method. This comment makes the linter happy. */
  public static void main(String[] args) throws Exception {
    DynamicConfigServer server = new DynamicConfigServer(8980);
    server.start();
    server.blockUntilShutdown();
  }

  private class DynamicConfigService extends ConfigurationServiceGrpc.ConfigurationServiceImplBase {
    @Override
    public void initialConfiguration(
        org.hypertrace.agent.config.Service.InitialConfigurationRequest request,
        io.grpc.stub.StreamObserver<
                org.hypertrace.agent.config.Service.InitialConfigurationResponse>
            responseObserver) {

      responseObserver.onNext(initialConfigurationResponse);
      responseObserver.onCompleted();
    }

    @Override
    public void updateConfiguration(
        org.hypertrace.agent.config.Service.UpdateConfigurationRequest request,
        io.grpc.stub.StreamObserver<org.hypertrace.agent.config.Service.UpdateConfigurationResponse>
            responseObserver) {
      responseObserver.onNext(updateConfigurationResponse);
      responseObserver.onCompleted();
    }
  }
}
