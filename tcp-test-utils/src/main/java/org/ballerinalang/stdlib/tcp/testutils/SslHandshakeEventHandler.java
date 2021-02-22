package org.ballerinalang.stdlib.tcp.testutils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

class SslHandshakeEventHandler extends ChannelInboundHandlerAdapter {

    private SimpleChannelInboundHandler<ByteBuf> handler;

    SslHandshakeEventHandler(SimpleChannelInboundHandler handler) {
        this.handler = handler;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            if (((SslHandshakeCompletionEvent) evt).isSuccess()) {
                ctx.pipeline().addLast(new FlowControlHandler());
                ctx.pipeline().addLast(handler);
                ctx.pipeline().remove(this);
            } else {
                System.out.println("SSL failure");
                ((SslHandshakeCompletionEvent) evt).cause().printStackTrace();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (handler instanceof SecureServerHandler) {
            System.out.println("Test server: " + cause.getMessage());
        } else {
            System.out.println("Test client" + cause.getMessage());
        }
        ctx.close();
    }
}
