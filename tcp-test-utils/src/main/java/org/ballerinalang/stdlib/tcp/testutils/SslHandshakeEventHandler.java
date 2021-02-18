package org.ballerinalang.stdlib.tcp.testutils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SslHandshakeEventHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SslHandshakeEventHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            if (((SslHandshakeCompletionEvent) evt).isSuccess()) {
                System.out.println("Handshake success" + ctx.channel().remoteAddress());
                log.info("Handshake success" + ctx.channel().remoteAddress());
                ctx.pipeline().addLast(new SecureServerHandler());
                ctx.pipeline().remove(this);
            } else {
                System.out.println("Handshake failure" + ctx.channel().remoteAddress());
                log.error("Handshake failure" + ctx.channel().remoteAddress(), ((SslHandshakeCompletionEvent) evt).cause());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause.getMessage() + ctx.channel().remoteAddress());
        log.error(cause.getMessage(), cause);
        ctx.close();
    }
}
