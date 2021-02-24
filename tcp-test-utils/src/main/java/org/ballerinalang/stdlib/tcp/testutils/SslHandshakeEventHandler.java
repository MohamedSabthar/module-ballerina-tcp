package org.ballerinalang.stdlib.tcp.testutils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

class SslHandshakeEventHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            if (((SslHandshakeCompletionEvent) evt).isSuccess()) {
                ctx.pipeline().addLast(new SecureServerHandler());
                ctx.pipeline().remove(this);
            } else {
                System.out.println("Test server handshake failure:" + ((SslHandshakeCompletionEvent) evt).cause().getMessage());
            }
        } else {
            System.out.println("Test server handshake failure :" + ((SslHandshakeCompletionEvent) evt).cause().getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Test server: " + cause.getMessage());
        ctx.close();
    }
}
