package com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons;

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
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureResourceGroupCollector<T> implements Callable<List<Metric>> {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureResourceGroupCollector.class);
    private Azure azure;
    private Service service;
    private Account account;
    private MonitorContextConfiguration monitorContextConfiguration;
    private Configuration config;
    private String resourceGroup;
    private LongAdder requestCounter;
    private String metricPrefix;

    public AzureResourceGroupCollector(Builder builder) {
        this.azure = builder.azure;
        this.service = builder.service;
        this.account = builder.account;
        this.monitorContextConfiguration = builder.monitorContextConfiguration;
        this.config = builder.config;
        this.resourceGroup = builder.resourceGroup;
        this.metricPrefix = builder.metricPrefix;
        this.requestCounter = builder.requestCounter;
    }

    public List<Metric> call() {
        return collectMetrics();
    }

    private List<Metric> collectMetrics() {
        MonitorExecutorService executorService = null;
        try {
            List<T> servicesList = NameSpaceGroupFactory.getNamespaceGroup(azure, service.getServiceName(), resourceGroup);
            if (servicesList.size() > 0) {
                executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(config.getConcurrencyConfig().getNoOfResourceGroupThreads()));
                List<FutureTask<List<Metric>>> tasks = buildFutureTasks(executorService, servicesList);
                return CommonUtilities.collectFutureMetrics(tasks, config.getConcurrencyConfig().getThreadTimeout(), "AzureResourceGroupCollector");
            }
        } catch (Exception e) {
            LOGGER.error("Exception caught in AzureResourceGroupCollector for resourceGroup {}", resourceGroup, e);
        } finally {
            executorService.shutdown();
        }
        return Lists.newArrayList();
    }

    private List<FutureTask<List<Metric>>> buildFutureTasks(MonitorExecutorService executorService, List<T> servicesList) {
        List<FutureTask<List<Metric>>> tasks = Lists.newArrayList();
        for (T serviceQueried : servicesList) {
            try {
                LOGGER.debug("starting AzureMetricCollector task for {}", resourceGroup);
                AzureMetricsCollector<T> metricsCollectorTask = new AzureMetricsCollector.Builder()
                        .withAzure(azure)
                        .withService(service)
                        .withAccount(account)
                        .withMonitorContextConfiguration(monitorContextConfiguration)
                        .withConfig(config)
                        .withMetricPrefix(metricPrefix)
                        .withRequestCounter(requestCounter)
                        .build();
                metricsCollectorTask.setService(serviceQueried);
                FutureTask<List<Metric>> metricsCollectorExecutorTask = new FutureTask(metricsCollectorTask);
                executorService.submit("AzureResourceGroupCollector", metricsCollectorExecutorTask);
                tasks.add(metricsCollectorExecutorTask);
            } catch (Exception e) {
                LOGGER.error("Exception while collecting metrics for resourceGroup {}, service {}, accountName {}", resourceGroup, service, account.getDisplayName(), e);
            }
        }
        return tasks;
    }

    public static class Builder {
        private Azure azure;
        private Service service;
        private Account account;
        private MonitorContextConfiguration monitorContextConfiguration;
        private Configuration config;
        private String resourceGroup;
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

        public Builder withResourceGroup(String resourceGroup) {
            this.resourceGroup = resourceGroup;
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

        public AzureResourceGroupCollector build() {
            return new AzureResourceGroupCollector(this);
        }
    }

}




