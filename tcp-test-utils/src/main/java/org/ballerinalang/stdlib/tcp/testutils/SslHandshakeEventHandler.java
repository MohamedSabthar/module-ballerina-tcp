package org.ballerinalang.stdlib.tcp.testutils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

class SslHandshakeEventHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            if (((SslHandshakeCompletionEvent) evt).isSuccess()) {
                System.out.println("Handshake success" + ctx.channel().remoteAddress());
                ctx.pipeline().addLast(new SecureServerHandler());
                ctx.pipeline().remove(this);
            } else {
                System.out.println("Handshake failure" + ctx.channel().remoteAddress());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause.getMessage() + ctx.channel().remoteAddress());
        ctx.close();
    }
}
