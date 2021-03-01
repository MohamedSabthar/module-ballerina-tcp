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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.concurrent.TimeUnit;

/**
 * Handler to handle Listener write timeout.
 */
public class ListenerWriteTimeoutHandler extends ClientWriteTimeoutHandler {
    private TcpService tcpService;


    public ListenerWriteTimeoutHandler(long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit,
                                       TcpService tcpService) {
        super(readerIdleTime, writerIdleTime, allIdleTime, unit);
        this.tcpService = tcpService;
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (evt == IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT || evt == IdleStateEvent.WRITER_IDLE_STATE_EVENT) {
            ctx.fireUserEventTriggered(new ListenerWriteIdleStateEvent(evt.state(), true, writeChannelFuture,
                    tcpService));
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }
}
