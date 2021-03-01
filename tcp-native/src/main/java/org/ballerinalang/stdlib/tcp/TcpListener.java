/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.tcp;

import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * {@link TcpListener} creates the tcp client and handles all the network operations.
 */
public class TcpListener {

    private Channel channel;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private SslContext sslContext;

    public TcpListener(InetSocketAddress localAddress, EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                       Future callback, TcpService tcpService, BMap<BString, Object> secureSocket) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        ServerBootstrap listenerBootstrap = new ServerBootstrap();

        listenerBootstrap.group(this.bossGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new ChannelInitializer<ServerSocketChannel>() {
                    @Override
                    protected void initChannel(ServerSocketChannel channel) throws Exception {
                        if (secureSocket != null) {
                            sslContext = getSslContext(secureSocket);
                        }
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        callback.complete(Utils.createSocketError(cause.getMessage()));
                        ctx.close();
                    }
                })
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        TcpListenerHandler tcpListenerHandler = new TcpListenerHandler(tcpService);
                        if (secureSocket != null) {
                            setSslHandler(channel, sslContext, tcpListenerHandler);
                        } else {
                            channel.pipeline().addLast(Constants.LISTENER_HANDLER, tcpListenerHandler);
                        }
                    }
                })
                .bind(localAddress)
                .addListener((ChannelFutureListener) channelFuture -> {
                    if (channelFuture.isSuccess()) {
                        channel = channelFuture.channel();
                        callback.complete(null);
                    } else {
                        callback.complete(Utils.createSocketError("Error initializing the server."));
                    }
                });
    }

    private SslContext getSslContext(BMap<BString, Object> secureSocket) throws IOException {
        BMap<BString, Object> certificate = (BMap<BString, Object>) secureSocket.getMapValue(StringUtils
                .fromString(Constants.CERTIFICATE));
        BMap<BString, Object> privateKey = (BMap<BString, Object>) secureSocket.getMapValue(StringUtils
                .fromString(Constants.PRIVATE_KEY));
        BMap<BString, Object> protocol = (BMap<BString, Object>) secureSocket.getMapValue(StringUtils
                .fromString(Constants.PROTOCOL));
        String[] protocolVersions = protocol == null ? new String[]{} : protocol.getArrayValue(StringUtils.
                fromString(Constants.PROTOCOL_VERSIONS)).getStringArray();
        String[] ciphers = secureSocket.getArrayValue(StringUtils.fromString(Constants.CIPHERS)).getStringArray();

        SSLConfig sslConfig = new SSLConfig();
        sslConfig.setServerCertificates(new File(certificate.getStringValue(StringUtils
                .fromString(Constants.CERTIFICATE_PATH)).getValue()));
        sslConfig.setServerKeyFile(new File(privateKey.getStringValue(StringUtils
                .fromString(Constants.PRIVATE_KEY_PATH)).getValue()));

        if (protocolVersions.length > 0) {
            sslConfig.setEnableProtocols(protocolVersions);
        }
        if (ciphers != null && ciphers.length > 0) {
            sslConfig.setCipherSuites(ciphers);
        }

        SSLHandlerFactory sslHandlerFactory = new SSLHandlerFactory(sslConfig);
        return sslHandlerFactory.createContextForServer();
    }

    private void setSslHandler(Channel channel, SslContext sslContext, TcpListenerHandler tcpListenerHandler) {
        SslHandler sslHandler = sslContext.newHandler(channel.alloc());

        channel.pipeline().addFirst(Constants.SSL_HANDLER, sslHandler);
        channel.pipeline().addLast(Constants.SSL_HANDSHAKE_HANDLER,
                new SslHandshakeListenerEventHandler(tcpListenerHandler));
    }

    // Invoke when the caller call writeBytes
    public static void send(byte[] bytes, Channel channel, Future callback, TcpService tcpService) {
        if (!tcpService.getIsCallerClosed() && channel.isActive()) {
            WriteFlowController writeFlowController = new WriteFlowController(Unpooled.wrappedBuffer(bytes), callback);
            TcpListenerHandler tcpListenerHandler = (TcpListenerHandler) channel.pipeline()
                    .get(Constants.LISTENER_HANDLER);
            tcpListenerHandler.addWriteFlowControl(writeFlowController);
            tcpListenerHandler.addBalWriteCallback(callback);
            ClientWriteTimeoutHandler clientWriteTimeoutHandler = new ClientWriteTimeoutHandler(0,
                    tcpService.getWriteTimeout(), 0, TimeUnit.MILLISECONDS, callback);
            channel.pipeline().addFirst(callback.hashCode() + Constants.WRITE_TIMEOUT_HANDLER,
                    clientWriteTimeoutHandler);
            if (channel.isWritable()) {
                writeFlowController.writeData(channel, tcpListenerHandler.getWriteFlowControllers(),
                        tcpListenerHandler.getBalWriteCallbacks());
            }
        } else {
            callback.complete(Utils.createSocketError("Socket connection already closed."));
        }
    }

    // Invoke when the listener onBytes return readonly & byte[]
    public static void send(byte[] bytes, Channel channel, TcpService tcpService) {
        if (!tcpService.getIsCallerClosed() && channel.isActive()) {
            WriteFlowControllerService writeFlowController =
                    new WriteFlowControllerService(Unpooled.wrappedBuffer(bytes), tcpService);
            TcpListenerHandler tcpListenerHandler = (TcpListenerHandler) channel
                    .pipeline().get(Constants.LISTENER_HANDLER);
            tcpListenerHandler.addWriteFlowControl(writeFlowController);
            tcpListenerHandler.addWriteService(tcpService);
            ListenerWriteTimeoutHandler listenerWriteTimeoutHandler = new ListenerWriteTimeoutHandler(0,
                    tcpService.getWriteTimeout(), 0, TimeUnit.MILLISECONDS, tcpService);
            channel.pipeline().addFirst(tcpService.getWriteTimeOutHandlerId() + Constants.WRITE_TIMEOUT_HANDLER,
                    listenerWriteTimeoutHandler);
            if (channel.isWritable()) {
                writeFlowController.writeData(channel, tcpListenerHandler.getWriteFlowControllers(),
                        tcpListenerHandler.getWriteServices());
            }
        } else {
            Dispatcher.invokeOnError(tcpService, "Socket connection already closed.");
        }
    }

    // Pause the network read operation until the onConnect method get invoked
    public static void pauseRead(Channel channel) {
        channel.config().setAutoRead(false);
    }

    // Resume the network read operation after onConnect execution completed
    public static void resumeRead(Channel channel) {
        channel.config().setAutoRead(true);
    }

    // Close caller
    public static void close(Channel channel, Future callback) {
        if (channel != null) {
            channel.close().addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    callback.complete(null);
                } else {
                    callback.complete(Utils.createSocketError("Failed to close the client connection"));
                }
            });
        }
    }

    // Shutdown the server
    public void close(Future callback) {
        channel.close().addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                callback.complete(null);
            } else {
                callback.complete(Utils.createSocketError("Failed to gracefully shutdown the Listener."));
            }
        });
    }
}
