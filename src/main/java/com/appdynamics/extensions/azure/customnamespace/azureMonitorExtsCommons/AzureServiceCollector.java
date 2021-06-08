/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons;

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
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

public class AzureServiceCollector implements Callable<List<Metric>> {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureServiceCollector.class);
    private Azure azure;
    private Service service;
    private Account account;
    private MonitorContextConfiguration monitorContextConfiguration;
    private Configuration config;
    private LongAdder requestCounter;
    private String metricPrefix;


    public AzureServiceCollector(Builder builder) {
        this.azure = builder.azure;
        this.service = builder.service;
        this.account = builder.account;
        this.monitorContextConfiguration = builder.monitorContextConfiguration;
        this.config = builder.config;
        this.requestCounter = builder.requestCounter;
        this.metricPrefix = builder.metricPrefix;
    }

    @Override
    public List<Metric> call() {
        return collectServerStatistics();
    }

    private List<Metric> collectServerStatistics() {
        List<Metric> metrics = Lists.newArrayList();
        MonitorExecutorService executorService = null;
        try {
            executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(config.getConcurrencyConfig().getNoOfServiceCollectorThreads()));
            List<FutureTask<List<Metric>>> resourceGroupFutureTask = buildFutureTasks(executorService);
            metrics = CommonUtilities.collectFutureMetrics(resourceGroupFutureTask, config.getConcurrencyConfig().getThreadTimeout(), "AzureServiceCollector");
        } catch (Exception e) {
            LOGGER.error("Error while collecting stats for account{} and server {}", account.getDisplayName(), service.getServiceName(), e);
        } finally {
            executorService.shutdown();
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
                    AzureResourceGroupCollector resourceGroupTask = new AzureResourceGroupCollector.Builder()
                            .withAzure(azure)
                            .withService(service)
                            .withAccount(account)
                            .withMonitorContextConfiguration(monitorContextConfiguration)
                            .withConfig(config)
                            .withResourceGroup(resourceGroup.name())
                            .withMetricPrefix(metricPrefix)
                            .withRequestCounter(requestCounter)
                            .build();
                    FutureTask<List<Metric>> resourceGroupExecutorTask = new FutureTask(resourceGroupTask);
                    executorService.submit("AzureServiceCollector", resourceGroupExecutorTask);
                    futureTasks.add(resourceGroupExecutorTask);
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

    public static class Builder {
        private Azure azure;
        private Service service;
        private Account account;
        private MonitorContextConfiguration monitorContextConfiguration;
        private Configuration config;
        private String metricPrefix;
        private LongAdder requestCounter;

        public Builder withAzure(Azure azure) {
            this.azure = azure;
            return this;
        }

        public Builder withService(Service service) {
            this.service = service;
            return this;
        }

        public Builder withAccount(Account account) {
            this.account = account;
            return this;
        }

        public Builder withMonitorContextConfiguration(MonitorContextConfiguration monitorContextConfiguration) {
            this.monitorContextConfiguration = monitorContextConfiguration;
            return this;
        }

        public Builder withConfig(Configuration config) {
            this.config = config;
            return this;
        }

        public Builder withMetricPrefix(String metricPrefix) {
            this.metricPrefix = metricPrefix;
            return this;
        }

        public Builder withRequestCounter(LongAdder requestCounter) {
            this.requestCounter = requestCounter;
            return this;
        }

        public AzureServiceCollector build() {
            return new AzureServiceCollector(this);
        }
    }
}