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

package org.ballerinalang.stdlib.tcp.nativelistener;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BObject;
import io.netty.channel.Channel;
import org.ballerinalang.stdlib.tcp.Constants;
import org.ballerinalang.stdlib.tcp.Dispatcher;
import org.ballerinalang.stdlib.tcp.TcpListener;
import org.ballerinalang.stdlib.tcp.TcpService;
import org.ballerinalang.stdlib.tcp.Utils;

/**
 * Native implementation of TCP caller.
 */
public class Caller {

    public static Object externWriteBytes(Environment env, BObject caller, BArray data) {
        final Future callback = env.markAsync();
        byte[] byteContent = data.getBytes();
        Channel channel = (Channel) caller.getNativeData(Constants.CHANNEL);
        TcpService tcpService = (TcpService) caller.getNativeData(Constants.SERVICE);
        TcpListener.send(byteContent, channel, callback, tcpService);
        return null;
    }

    public static Object externClose(Environment env, BObject caller) {
        final Future callback = env.markAsync();

        Channel channel = (Channel) caller.getNativeData(Constants.CHANNEL);
        TcpService tcpService = (TcpService) caller.getNativeData(Constants.SERVICE);
        tcpService.setIsCallerClosed(true);
        try {
            TcpListener.close(channel, callback);
            Dispatcher.invokeOnClose(tcpService);
        } catch (Exception e) {
            callback.complete(Utils.createSocketError(e.getMessage()));
        }
        return null;
    }
}
