/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.apim.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Thread pool holder to execute asynchronous tasks
 */
public class AsyncExecutor {

    public static final int DEFAULT_CORE_THREADS = 10;
    public static final int DEFAULT_MAX_THREADS = 20;
    public static final int DEFAULT_KEEP_ALIVE = 30;
    public static final int DEFAULT_QUEUE_LENGTH = 10;

    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * Initialize with given configuration
     *
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param queueLength
     */
    public AsyncExecutor (int corePoolSize, int maximumPoolSize, long keepAliveTime, int queueLength) {
        threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                                    keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(queueLength));
    }

    /**
     * Initialize with default configuration
     */
    public AsyncExecutor () {
        threadPoolExecutor = new ThreadPoolExecutor(DEFAULT_CORE_THREADS, DEFAULT_MAX_THREADS, DEFAULT_KEEP_ALIVE, TimeUnit.SECONDS,
                                                            new LinkedBlockingQueue<Runnable>(DEFAULT_QUEUE_LENGTH));
    }

    /**
     * This will execute given task in async thread pool
     *
     * @param runnable
     */
    public void executeAsync(Runnable runnable) {
        threadPoolExecutor.execute(runnable);
    }
}
