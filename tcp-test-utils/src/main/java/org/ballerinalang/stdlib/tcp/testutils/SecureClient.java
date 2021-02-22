package org.ballerinalang.stdlib.tcp.testutils;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import java.io.File;
import java.net.InetSocketAddress;

public class SecureClient {
    private static int PORT = 7523;
    private static EventLoopGroup group;

    public static Object run() {
        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .localAddress(new InetSocketAddress(PORT))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            // Set ssl handler
                            SslContext sslContext = SslContextBuilder
                                    .forClient().trustManager(new File("../tcp-test-utils/etc/cert.pem")).build();
                            SslHandler handler = sslContext.newHandler(ch.alloc());
                            handler.engine().setEnabledProtocols(new String[]{"TLSv1.2"});
                            handler.engine().setEnabledCipherSuites(new String[]{"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"});
                            handler.setHandshakeTimeoutMillis(20_000); // set the handshake timeout value to 20sec
                            ch.pipeline().addFirst(handler);
                            ch.pipeline().addLast(new SslHandshakeEventHandler(new SecureClientHandler()));
                            System.out.println("SSL handler added");
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                            System.out.println("Test client: " + cause.getMessage());
                            ctx.close();
                        }
                    });

            ChannelFuture f = b.bind().sync();

            f.channel().write(Unpooled.wrappedBuffer("Hello from test client".getBytes())).sync()
                    .addListener((ChannelFutureListener) channelFuture -> {
                        if (channelFuture.isSuccess()) {
                            System.out.println("Data written by client");
                        } else {
                            System.out.println("Failed to write data : " + channelFuture.cause().getMessage());
                            channelFuture.cause().printStackTrace();
                        }
                    });
            f.channel().close().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
