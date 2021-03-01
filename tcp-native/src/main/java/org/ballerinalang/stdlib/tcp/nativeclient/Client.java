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

package org.ballerinalang.stdlib.tcp.nativeclient;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.netty.channel.Channel;
import org.ballerinalang.stdlib.tcp.Constants;
import org.ballerinalang.stdlib.tcp.Dispatcher;
import org.ballerinalang.stdlib.tcp.TcpClient;
import org.ballerinalang.stdlib.tcp.TcpFactory;
import org.ballerinalang.stdlib.tcp.TcpListener;
import org.ballerinalang.stdlib.tcp.TcpService;
import org.ballerinalang.stdlib.tcp.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Native function implementations of the TCP Client.
 *
 * @since 1.1.0
 */
public class Client {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    public static Object externInit(Environment env, BObject client, BString remoteHost, int remotePort,
                                    BMap<BString, Object> config) {
        final Future balFuture = env.markAsync();

        BString host = config.getStringValue(StringUtils.fromString(Constants.CONFIG_LOCALHOST));
        InetSocketAddress remoteAddress = new InetSocketAddress(remoteHost.getValue(), remotePort);

        InetSocketAddress localAddress;
        if (host == null) {
            // A port number of zero will let the system pick up an ephemeral port in a bind operation.
            localAddress = new InetSocketAddress(0);
        } else {
            localAddress = new InetSocketAddress(host.getValue(), 0);
        }

        long timeout = config.getIntValue(StringUtils.fromString(Constants.CONFIG_READ_TIMEOUT));
        client.addNativeData(Constants.CONFIG_READ_TIMEOUT, timeout);
        BMap<BString, Object> secureSocket = (BMap<BString, Object>) config.getMapValue(StringUtils.
                fromString(Constants.SECURE_SOCKET));

        TcpClient tcpClient = TcpFactory.getInstance().
                createTcpClient(localAddress, remoteAddress, balFuture, secureSocket, client);
        client.addNativeData(Constants.CLIENT, tcpClient);

        return null;
    }

    public static Object externReadBytes(Environment env, BObject client) {
        final Future balFuture = env.markAsync();

        long readTimeOut = (long) client.getNativeData(Constants.CONFIG_READ_TIMEOUT);
        TcpClient tcpClient = (TcpClient) client.getNativeData(Constants.CLIENT);
        tcpClient.readData(readTimeOut, balFuture);

        return null;
    }

    public static Object externWriteBytes(Environment env, BObject client, BArray content) {
        final Future balFuture = env.markAsync();
        byte[] byteContent = content.getBytes();

        TcpClient tcpClient = (TcpClient) client.getNativeData(Constants.CLIENT);

        // if client have channel as native data then it's used inside the listener
        if (tcpClient != null) {
            tcpClient.writeData(byteContent, balFuture);
        } else {
            BObject caller = client;
            Channel channel = (Channel) caller.getNativeData(Constants.CHANNEL);
            TcpService tcpService = (TcpService) caller.getNativeData(Constants.SERVICE);
            TcpListener.send(byteContent, channel, balFuture, tcpService);
        }

        return null;
    }

    public static Object externClose(Environment env, BObject client) {
        final Future balFuture = env.markAsync();

        TcpClient tcpClient = (TcpClient) client.getNativeData(Constants.CLIENT);


        // if client have channel as native data then it's used inside the listener
        if (tcpClient != null) {
            tcpClient.close(balFuture);
        } else {
            BObject caller = client;
            Channel channel = (Channel) caller.getNativeData(Constants.CHANNEL);
            TcpService tcpService = (TcpService) caller.getNativeData(Constants.SERVICE);
            tcpService.setIsCallerClosed(true);
            try {
                TcpListener.close(channel, balFuture);
                Dispatcher.invokeOnClose(tcpService);
            } catch (Exception e) {
                balFuture.complete(Utils.createSocketError(e.getMessage()));
            }
        }
        return null;
    }
}
