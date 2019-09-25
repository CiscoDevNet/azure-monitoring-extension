/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.config;


public class TaskSchedule {

    private int numberOfThreads;
    private int taskDelaySeconds;

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public int getTaskDelaySeconds() {
        return taskDelaySeconds;
    }

    public void setTaskDelaySeconds(int taskDelaySeconds) {
        this.taskDelaySeconds = taskDelaySeconds;
    }
}
