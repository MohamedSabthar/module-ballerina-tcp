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

import io.netty.channel.ChannelFuture;
import io.netty.handler.timeout.IdleState;

/**
 * ListenerWriteIdleStateEvent to handle Listener write timeout.
 */
public class ListenerWriteIdleStateEvent extends ClientWriterIdleStateEvent {
    private TcpService tcpService;

    protected ListenerWriteIdleStateEvent(IdleState state, boolean first, ChannelFuture writeChannelFuture,
                                          TcpService tcpService) {
        super(state, first, writeChannelFuture);
        this.tcpService = tcpService;
    }

    public TcpService getTcpService() {
        return tcpService;
    }
}
