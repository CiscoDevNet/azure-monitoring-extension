package com.appdynamics.extensions.azure.customnamespace;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons.AzureResourceGroupCollector;
import com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons.TaskBuilder;
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
import com.microsoft.azure.management.resources.ResourceGroup;
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
public class AzureServiceCollector extends TaskBuilder {
    Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureServiceCollector.class);
    public Service service;

    public AzureServiceCollector(Azure azure, Account account, MonitorContextConfiguration monitorContextConfiguration, Configuration config, MetricWriteHelper metricWriteHelper, Service service, String metricPrefix) {
        super(azure, account, monitorContextConfiguration, config, metricWriteHelper, metricPrefix);
        this.service = service;
    }

    @Override
    public List<Metric> call() {
        return collectServerStatistics();
    }

    private List<Metric> collectServerStatistics() {
        List<Metric> metrics = Lists.newArrayList();
        try {
            MonitorExecutorService executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(config.getConcurrencyConfig().getNoOfServiceCollectorThreads()));
            List<FutureTask<List<Metric>>> resourceGroupFutureTask = buildFutureTasks(executorService);
            metrics = CommonUtilities.collectFutureMetrics(resourceGroupFutureTask, config.getConcurrencyConfig().getThreadTimeout(), "AzureServiceCollector");
        } catch (Exception e) {
            LOGGER.error("Error while collecting stats for account{} and server {}", account.getDisplayName(), service.getServiceName(), e);
        } finally {
            return metrics;
        }
    }

    private List<FutureTask<List<Metric>>> buildFutureTasks(MonitorExecutorService executorService) {
        List<FutureTask<List<Metric>>> futureTasks = Lists.newArrayList();
        try {
            List<String> confResourceGroups = service.getResourceGroups();
            List<ResourceGroup> filteredResourceGroups = filteredResourceGroups(azure.resourceGroups().list(), confResourceGroups);
            LOGGER.debug("Filtered resourceGroups are {}", filteredResourceGroups);
            for (ResourceGroup resourceGroup : filteredResourceGroups) {
                try {
                    AzureResourceGroupCollector accountTask = new AzureResourceGroupCollector(azure, account, service, monitorContextConfiguration, config, metricWriteHelper, resourceGroup.name(), metricPrefix);
                    FutureTask<List<Metric>> accountExecutorTask = new FutureTask(accountTask);
                    executorService.submit("AzureServiceCollector", accountExecutorTask);
                    futureTasks.add(accountExecutorTask);
                } catch (Exception e) {
                    LOGGER.error("Error while collecting metrics for resourceGroup {}", resourceGroup.name(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception while querying for a resourceGroup of account {} and service{}", account.getDisplayName(), service.getServiceName(), e);
        } finally {
            return futureTasks;
        }
    }

    private List<ResourceGroup> filteredResourceGroups(List<ResourceGroup> resourceGroups, List<String> confResourceGroups) {
        List<ResourceGroup> filteredResourceGroup = Lists.newArrayList();
        for (ResourceGroup resourceGroup : resourceGroups) {
            if (CommonUtilities.checkStringPatternMatch(resourceGroup.name(), confResourceGroups))
                filteredResourceGroup.add(resourceGroup);
            else
                LOGGER.debug("No match for resourceGroup {}, Excluding it", resourceGroup.name());
        }
        return filteredResourceGroup;
    }
}
