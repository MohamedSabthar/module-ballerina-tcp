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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.ssl.SslCloseCompletionEvent;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.ballerinalang.stdlib.tcp.nativeclient.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle SSL handshake event of TCP Client.
 */
public class SslHandshakeClientEventHandler extends ChannelInboundHandlerAdapter {
    private TcpClientHandler tcpClientHandler;
    private Future balClientInitCallback;
    private static final Logger log = LoggerFactory.getLogger(Client.class);

    public SslHandshakeClientEventHandler(TcpClientHandler handler, Future callback) {
        tcpClientHandler = handler;
        this.balClientInitCallback = callback;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Utils.print("SslEventHandler channelActive: local: " + ctx.channel().localAddress() + "remote: "
                + ctx.channel().remoteAddress());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        Utils.print("Client userEventTriggered: remote: " + ctx.channel().remoteAddress() +
                " local: " + ctx.channel().localAddress());
        if (event instanceof SslHandshakeCompletionEvent) {
            if (((SslHandshakeCompletionEvent) event).isSuccess()) {
                ctx.pipeline().addLast(Constants.FLOW_CONTROL_HANDLER, new FlowControlHandler());
                ctx.pipeline().addLast(Constants.CLIENT_HANDLER, tcpClientHandler);
                ctx.channel().config().setAutoRead(false);
                balClientInitCallback.complete(null);
                Utils.print("Callback complete D: " + balClientInitCallback.hashCode());
                ctx.pipeline().remove(this);
            } else {
                Utils.print("++++++++++++++++++++++++++++++++++++++");
                for (var a : ctx.pipeline().names()) {
                    Utils.print(a);
                }
                Utils.print("--------------------------------------");
                balClientInitCallback.complete(Utils.createSocketError(((SslHandshakeCompletionEvent) event).
                        cause().getMessage()));
                Utils.print("Callback complete E: " + balClientInitCallback.hashCode());
                balClientInitCallback = null;
                ctx.close();
            }
        } else if (!(event instanceof SslCloseCompletionEvent)) {
            log.warn("Unexpected user event triggered");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error while SSL handshake: " + cause.getMessage());
        if (cause instanceof DecoderException && balClientInitCallback != null) {
            balClientInitCallback.complete(Utils.createSocketError(cause.getMessage()));
            Utils.print("Callback complete F: " + balClientInitCallback.hashCode());
            ctx.close();
        }
    }
}
