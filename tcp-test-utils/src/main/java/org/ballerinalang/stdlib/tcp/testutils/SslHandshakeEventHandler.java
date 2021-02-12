package org.ballerinalang.stdlib.tcp.testutils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.ballerinalang.stdlib.tcp.Constants;

class SslHandshakeEventHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        SslHandler sslHandler = (SslHandler) ctx.pipeline().get(Constants.SSL_HANDLER);
        sslHandler.handshakeFuture().addListener(future -> {
           if (!future.isSuccess()) {
               future.cause().printStackTrace();
           }
        });
        if (evt instanceof SslHandshakeCompletionEvent) {
            if (((SslHandshakeCompletionEvent) evt).isSuccess()) {
                ctx.pipeline().addLast(new SecureServerHandler());
                ctx.pipeline().remove(this);
                System.out.println("Handshake success" + ctx.channel().remoteAddress());
            }
        } else {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Test server: " + cause.getMessage());
        ctx.close();
    }
}
