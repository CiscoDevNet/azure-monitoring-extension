package com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.azure.customnamespace.config.Configuration;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.microsoft.azure.management.Azure;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public abstract class TaskBuilder implements Callable<List<Metric>> {
    protected Azure azure;
    protected Map<String, ?> account;
    protected MonitorContextConfiguration monitorContextConfiguration;
    protected Configuration config;
    protected MetricWriteHelper metricWriteHelper;
    protected String metricPrefix;

    public TaskBuilder(Azure azure, Map<String, ?> account, MonitorContextConfiguration monitorContextConfiguration, Configuration config, MetricWriteHelper metricWriteHelper, String metricPrefix) {
        this.azure = azure;
        this.account = account;
        this.monitorContextConfiguration = monitorContextConfiguration;
        this.config = config;
        this.metricWriteHelper = metricWriteHelper;
        this.metricPrefix = metricPrefix;
    }
}