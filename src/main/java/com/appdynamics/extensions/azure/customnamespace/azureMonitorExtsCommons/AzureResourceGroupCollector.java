package com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.azure.customnamespace.AzureNamespaceGroupFactory.NameSpaceGroupFactory;
import com.appdynamics.extensions.azure.customnamespace.config.Account;
import com.appdynamics.extensions.azure.customnamespace.config.Configuration;
import com.appdynamics.extensions.azure.customnamespace.config.Service;
import com.appdynamics.extensions.azure.customnamespace.utils.CommonUtilities;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.executorservice.MonitorThreadPoolExecutor;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.microsoft.azure.management.Azure;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureResourceGroupCollector<T> extends TaskBuilder {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger("AzureResourceGroupCollector.class");
    private String resourceGroup;
    private Service service;

    public AzureResourceGroupCollector(Azure azure, Account account, Service service, MonitorContextConfiguration monitorContextConfiguration, Configuration config, MetricWriteHelper metricWriteHelper, String resourceGroup, String metricPrefix) {
        super(azure, account, monitorContextConfiguration, config, metricWriteHelper, metricPrefix);
        this.resourceGroup = resourceGroup;
        this.service = service;
    }

    public List<Metric> call() {
        return collectMetrics();
    }

    private List<Metric> collectMetrics() {
        try {
            List<T> servicesList = NameSpaceGroupFactory.getNamespaceGroup(azure, service.getServiceName(), resourceGroup);
            if (servicesList.size() > 0) {
                MonitorExecutorService executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(config.getConcurrencyConfig().getNoOfMetricsCollectorThreads()));
                List<FutureTask<List<Metric>>> tasks = buildFutureTasks(executorService, servicesList);
                return CommonUtilities.collectFutureMetrics(tasks, config.getConcurrencyConfig().getThreadTimeout(), "AzureResourceGroupCollector");
            }
        }catch (Exception e){
            LOGGER.error("Exeception caught in AzureResourceGroupCollector for resourceGroup {}", resourceGroup, e);
        }
        return Lists.newArrayList();
    }

    private List<FutureTask<List<Metric>>> buildFutureTasks(MonitorExecutorService executorService, List<T> servicesList) {
        List<FutureTask<List<Metric>>> tasks = Lists.newArrayList();
        for (T serviceQueried : servicesList) {
            try {
                LOGGER.debug("starting AzureMetricCollector task for {}", resourceGroup);
                AzureMetricsCollector<T> accountTask = new AzureMetricsCollector(azure, account, service, monitorContextConfiguration, config, metricWriteHelper, metricPrefix, serviceQueried);
                FutureTask<List<Metric>> accountExecutorTask = new FutureTask(accountTask);
                executorService.submit("AzureResourceGroupCollector", accountExecutorTask);
                tasks.add(accountExecutorTask);
            }catch (Exception e){
                LOGGER.error("Exception while collecting metrics for resourceGroup {}, service {}, accountName {}", resourceGroup, service, account.getDisplayName(), e);
            }
        }
        return tasks;
    }

}





