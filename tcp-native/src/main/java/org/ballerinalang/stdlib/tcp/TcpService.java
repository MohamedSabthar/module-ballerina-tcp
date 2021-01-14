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
public class TcpService {

    private final Runtime runtime;
    private final BObject service;
    private BObject connectionService;
    private final long timeout;

    public TcpService(Runtime runtime, BObject service, long timeout) {
        this.runtime = runtime;
        this.service = service;
        this.timeout = timeout;
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

    public long getTimeout() {
        return timeout;
    }
}
