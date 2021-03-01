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

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.values.BObject;

/**
 * Represent TcpService which used for invoking service remote methods.
 */
public class TcpService implements Cloneable {

    private final Runtime runtime;
    private final BObject service;
    private BObject connectionService;
    private boolean isCallerClosed;
    private long writeTimeout;
    private int writeTimeOutHandlerId; // used as writeTimeOutHandlerId+Constants.WRITE_TIMEOUT_HANDLER to
                                       // identify handler in pipeline

    public TcpService(Runtime runtime, BObject service, long writeTimeout) {
        this.runtime = runtime;
        this.service = service;
        this.writeTimeout = writeTimeout;
        this.writeTimeOutHandlerId = 0;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public BObject getService() {
        return service;
    }

    public void setConnectionService(BObject connectionService) {
        this.connectionService = connectionService;
    }

    public BObject getConnectionService() {
        return connectionService;
    }

    public boolean getIsCallerClosed() {
        return isCallerClosed;
    }

    public void setIsCallerClosed(boolean callerClosed) {
        isCallerClosed = callerClosed;
    }

    public void setWriteTimeOutHandlerId(int writeTimeOutHandlerId) {
        this.writeTimeOutHandlerId = writeTimeOutHandlerId;
    }

    public int getWriteTimeOutHandlerId() {
        return writeTimeOutHandlerId;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public long getWriteTimeout() {
        return writeTimeout;
    }
}
