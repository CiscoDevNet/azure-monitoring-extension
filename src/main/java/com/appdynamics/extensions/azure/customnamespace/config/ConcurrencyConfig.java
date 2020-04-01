/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.config;
/*
 * Copyright 2019. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

public class ConcurrencyConfig {

    private int noOfResourceGroupThreads;
    private int noOfServiceCollectorThreads;
    private int threadTimeout;

    public int getThreadTimeout() {
        return threadTimeout;
    }

    public void setThreadTimeout(int threadTimeout) {
        this.threadTimeout = threadTimeout;
    }

    public int getNoOfResourceGroupThreads() {
        return noOfResourceGroupThreads;
    }

    public void setNoOfResourceGroupThreads(int noOfResourceGroupThreads) {
        this.noOfResourceGroupThreads = noOfResourceGroupThreads;
    }

    public int getNoOfServiceCollectorThreads() {
        return noOfServiceCollectorThreads;
    }

    public void setNoOfServiceCollectorThreads(int noOfServiceCollectorThreads) {
        this.noOfServiceCollectorThreads = noOfServiceCollectorThreads;
    }
}
