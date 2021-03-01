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
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * {@link TcpClientHandler} is a ChannelInboundHandler implementation for tcp client.
 */
public class TcpClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private Future balReadCallback;
    private boolean isCloseTriggered = false;
    private LinkedList<WriteFlowController> writeFlowControllers = new LinkedList<>();
    private Map<Integer, Future> balWriteCallbacks = new HashMap<>();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!isCloseTriggered && balReadCallback != null) {
            balReadCallback.complete(Utils.createSocketError("Connection closed by the server."));
        }
        ctx.channel().close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ctx.channel().pipeline().remove(Constants.READ_TIMEOUT_HANDLER);
        if (balReadCallback != null) {
            balReadCallback.complete(Utils.returnReadOnlyBytes(msg));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (event == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT || event == IdleStateEvent.READER_IDLE_STATE_EVENT) {
            // return timeout error
            ctx.channel().pipeline().remove(Constants.READ_TIMEOUT_HANDLER);
            if (balReadCallback != null) {
                balReadCallback.complete(Utils.createSocketError("Read timed out"));
            }
        } else if (event instanceof ClientWriterIdleStateEvent) {
            ClientWriterIdleStateEvent writeEvent = (ClientWriterIdleStateEvent) event;
            ctx.pipeline().remove(writeEvent.getBalWriteCallback().hashCode() + Constants.WRITE_TIMEOUT_HANDLER);
            if (writeEvent.getWriteChannelFuture() != null) {
                writeEvent.getWriteChannelFuture().cancel(true);
            }
            if (balWriteCallbacks.containsKey(writeEvent.getBalWriteCallback().hashCode())) {
                writeEvent.getBalWriteCallback().complete(Utils.createSocketError("Write timed out"));
                balWriteCallbacks.remove(writeEvent.getBalWriteCallback().hashCode());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().pipeline().remove(Constants.READ_TIMEOUT_HANDLER);
        if (balReadCallback != null) {
            balReadCallback.complete(Utils.createSocketError(cause.getMessage()));
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        while (writeFlowControllers.size() > 0) {
            if (ctx.channel().isWritable()) {
                WriteFlowController writeFlowController = writeFlowControllers.getFirst();
                if (writeFlowController != null) {
                    writeFlowController.writeData(ctx.channel(), writeFlowControllers, balWriteCallbacks);
                }
            }
        }
    }

    public void setBalReadCallback(Future balReadCallback) {
        this.balReadCallback = balReadCallback;
    }

    public void addBalWriteCallback(Future balWriteCallback) {
        balWriteCallbacks.put(balWriteCallback.hashCode(), balWriteCallback);
    }

    public void setIsCloseTriggered() {
        isCloseTriggered = true;
    }

    public void addWriteFlowControl(WriteFlowController writeFlowController) {
        writeFlowControllers.addLast(writeFlowController);
    }

    public LinkedList<WriteFlowController> getWriteFlowControllers() {
        return writeFlowControllers;
    }

    public Map<Integer, Future> getBalWriteCallbacks() {
        return balWriteCallbacks;
    }
}

