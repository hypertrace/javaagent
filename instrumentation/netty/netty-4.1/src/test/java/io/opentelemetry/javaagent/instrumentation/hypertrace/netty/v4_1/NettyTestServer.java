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

package io.opentelemetry.javaagent.instrumentation.hypertrace.netty.v4_1;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class NettyTestServer {

  static final String RESPONSE_HEADER_NAME = "respheader";
  static final String RESPONSE_HEADER_VALUE = "respheadervalue";
  static final String RESPONSE_BODY = "{\"foo\": \"bar\"}";

  private static final LoggingHandler LOGGING_HANDLER =
      new LoggingHandler(NettyTestServer.class, LogLevel.DEBUG);

  private final List<Class<? extends ChannelHandler>> handlers;

  NettyTestServer(List<Class<? extends ChannelHandler>> handlers) {
    this.handlers = handlers;
  }

  private EventLoopGroup eventLoopGroup;

  public int create() throws IOException, InterruptedException {
    eventLoopGroup = new NioEventLoopGroup();

    ServerBootstrap serverBootstrap = new ServerBootstrap();
    serverBootstrap.group(eventLoopGroup);
    serverBootstrap
        .handler(LOGGING_HANDLER)
        .childHandler(
            new ChannelInitializer<Channel>() {
              @Override
              protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addFirst("logger", LOGGING_HANDLER);

                for (Class<? extends ChannelHandler> channelHandlerClass : handlers) {
                  ChannelHandler channelHandler = channelHandlerClass.newInstance();
                  pipeline.addLast(channelHandler);
                }

                pipeline.addLast(
                    new SimpleChannelInboundHandler() {

                      HttpRequest httpRequest;

                      @Override
                      protected void channelRead0(ChannelHandlerContext ctx, Object msg) {

                        if (msg instanceof HttpRequest) {
                          this.httpRequest = (HttpRequest) msg;
                        }

                        // write response after all content has been received otherwise
                        // server span is closed before request payload is captured
                        if (msg instanceof LastHttpContent && httpRequest != null) {
                          if (httpRequest.getUri().contains("get_no_content")) {
                            HttpResponse response =
                                new DefaultFullHttpResponse(
                                    HTTP_1_1, HttpResponseStatus.valueOf(204));
                            response.headers().add(RESPONSE_HEADER_NAME, RESPONSE_HEADER_VALUE);
                            response.headers().set(CONTENT_LENGTH, 0);
                            ctx.write(response);
                          } else if (httpRequest.getUri().contains("post")) {
                            ByteBuf responseBody = Unpooled.wrappedBuffer(RESPONSE_BODY.getBytes());
                            HttpResponse response =
                                new DefaultFullHttpResponse(
                                    HTTP_1_1, HttpResponseStatus.valueOf(200), responseBody);
                            response.headers().add(RESPONSE_HEADER_NAME, RESPONSE_HEADER_VALUE);
                            response.headers().add("Content-Type", "application-json");
                            response.headers().set(CONTENT_LENGTH, responseBody.readableBytes());
                            ctx.write(response);
                          }
                        }
                      }

                      @Override
                      public void channelReadComplete(ChannelHandlerContext ctx) {
                        ctx.flush();
                      }
                    });
              }
            })
        .channel(NioServerSocketChannel.class);

    ServerSocket socket;
    socket = new ServerSocket(0);
    int port = socket.getLocalPort();
    socket.close();

    serverBootstrap.bind(port).sync();
    return port;
  }

  public void stopServer() throws ExecutionException, InterruptedException {
    eventLoopGroup.shutdownGracefully().get();
  }
}
