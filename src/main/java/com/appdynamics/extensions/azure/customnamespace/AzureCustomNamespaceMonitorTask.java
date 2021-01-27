package com.appdynamics.extensions.azure.customnamespace;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.azure.customnamespace.azureAuthStore.AuthenticationFactory;
import com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons.AzureServiceCollector;
import com.appdynamics.extensions.azure.customnamespace.azureTarget.AzureTargetMonitorTask;
import com.appdynamics.extensions.azure.customnamespace.config.Account;
import com.appdynamics.extensions.azure.customnamespace.config.Configuration;
import com.appdynamics.extensions.azure.customnamespace.config.Service;
import com.appdynamics.extensions.azure.customnamespace.config.Target;
import com.appdynamics.extensions.azure.customnamespace.utils.AzureApiVersionStore;
import com.appdynamics.extensions.azure.customnamespace.utils.CommonUtilities;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.executorservice.MonitorThreadPoolExecutor;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.management.Azure;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureCustomNamespaceMonitorTask implements AMonitorTaskRunnable {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureCustomNamespaceMonitorTask.class);
    private String metricPrefix;
    private MonitorContextConfiguration monitorContextConfiguration;
    private Configuration config;
    private MetricWriteHelper metricWriteHelper;
    private Account account;
    private Azure azure;
    private AuthenticationResult authTokenResult;
    private LongAdder azureRequestCounter = new LongAdder();


    public AzureCustomNamespaceMonitorTask(MonitorContextConfiguration monitorContextConfiguration, Configuration config, MetricWriteHelper metricWriteHelper, Account account, String metricPrefix) {
        this.monitorContextConfiguration = monitorContextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.account = account;
        this.metricPrefix = metricPrefix;
        this.config = config;
    }

    public void run() {
        try {
            LOGGER.debug("[ACCOUNT={}] - Started processing account.", account.getDisplayName());
            azure = AuthenticationFactory.getAzure(account.getCredentials(), config.getEncryptionKey());
            
            if (azure == null)
                throw new Exception("Failed: built Azure object is null");
            else
                collectStatistics();

            LOGGER.debug("[ACCOUNT={}] - Completed processing account.", account.getDisplayName());
        } catch (IOException IOe) {
            LOGGER.error("[ACCOUNT={}] - Error in Authentication of the Azure client", account.getDisplayName(), IOe);
        } catch (Exception e) {
            LOGGER.error("[ACCOUNT={}] - Exception while creating azure client", account.getDisplayName(), e);
        }
        onTaskComplete();
    }

    private void collectStatistics() {
        List<Metric> metrics = Lists.newArrayList();
        try {
            MonitorExecutorService executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(config.getConcurrencyConfig().getNoOfServiceCollectorThreads()));
            List<FutureTask<List<Metric>>> resourceGroupFutureTask = buildFutureTasks(executorService);
            metrics = CommonUtilities.collectFutureMetrics(resourceGroupFutureTask, config.getConcurrencyConfig().getThreadTimeout(), "AzureCustomNamespaceMonitorTask");

            //Targets Support with HttpClient
            List<Target> resourceTargets = account.getTargets();
            if (resourceTargets != null)
                metrics.addAll(initTargetMetricsCollection(resourceTargets));
        } catch (Exception e) {
            LOGGER.error("[ACCOUNT={}] - Error while collecting stats.", account.getDisplayName(), e);
        } finally {
            Metric apiCallsMetric = new Metric("Azure API Calls", Double.toString(this.azureRequestCounter.doubleValue()), metricPrefix + "Azure API Calls");
            metrics.add(apiCallsMetric);
            metricWriteHelper.transformAndPrintMetrics(metrics);
        }
    }

    private List<FutureTask<List<Metric>>> buildFutureTasks(MonitorExecutorService executorService) {
        List<FutureTask<List<Metric>>> futureTasks = Lists.newArrayList();
        try {
            List<Service> services = account.getServices();
            for (Service service : services) {
                try {
                    LOGGER.debug("[ACCOUNT={}, SERVICE={}] - Started processing service",  account.getDisplayName(), service.getServiceName());
                    AzureServiceCollector serviceCollectorTask = new AzureServiceCollector.Builder()
                            .withAzure(azure)
                            .withService(service)
                            .withAccount(account)
                            .withMonitorContextConfiguration(monitorContextConfiguration)
                            .withConfig(config)
                            .withMetricPrefix(metricPrefix)
                            .withRequestCounter(azureRequestCounter)
                            .build();
                    FutureTask<List<Metric>> serviceExecutorTask = new FutureTask<>(serviceCollectorTask);
                    executorService.submit("AzureCustomNamespaceMonitorTask", serviceExecutorTask);
                    futureTasks.add(serviceExecutorTask);
                } catch (Exception e) {
                    LOGGER.error("[ACCOUNT={}, SERVICE={}] - Error while collecting metrics for service", account.getDisplayName(), service.getServiceName(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[ACCOUNT={}] - Exception while querying for services.", account.getDisplayName(), e);
        } finally {
            
        }
        return futureTasks;
    }


    private List<FutureTask<List<Metric>>> buildFutureTargetTasks(MonitorExecutorService executorService, List<Target> targets) {
        List<FutureTask<List<Metric>>> futureTasks = Lists.newArrayList();
        try {
            authTokenResult = AuthenticationFactory.getAccessTokenFromUserCredentials();
            for (Target target : targets) {
                LOGGER.debug("[ACCOUNT={}, TARGET={}] - Started processing target",  account.getDisplayName(), target.getDisplayName());
                try {
                    AzureTargetMonitorTask targetTask = new AzureTargetMonitorTask.Builder()
                                .withTarget(target)
                                .withAccount(account)
                                .withAuthenticationResult(authTokenResult)
                                .withConfiguration(config)
                                .withSubscriptionId(account.getCredentials().getSubscriptionId())
                                .withMetricPrefix(metricPrefix)
                                .withRequestCounter(azureRequestCounter)
                                .build();

                    FutureTask<List<Metric>> targetExecutorTask = new FutureTask(targetTask);
                    executorService.submit("AzureTargetMonitorTask", targetExecutorTask);
                    futureTasks.add(targetExecutorTask);
                } catch (Exception e) {
                    LOGGER.error("[ACCOUNT={}, TARGET={}] - Error while collecting metrics", account.getDisplayName(), target.getDisplayName(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[ACCOUNT={}] - Exception while querying targets ||{}||", account.getDisplayName(), targets, e);
        } finally {
            
        }
        return futureTasks;
    }

    private List<Metric> initTargetMetricsCollection(List<Target> targets) {
        List<Metric> metrics = Lists.newArrayList();
        try {
            MonitorExecutorService executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(targets.size()));
            List<FutureTask<List<Metric>>> targetFutureTask = buildFutureTargetTasks(executorService, account.getTargets());
            metrics = CommonUtilities.collectFutureMetrics(targetFutureTask, config.getConcurrencyConfig().getThreadTimeout(), "AzureCustomNamespaceMonitorTask");
        } catch (Exception e) {
            LOGGER.error("[ACCOUNT={}] - Exception occurred while collecting metrics for targets ||{}||", account.getDisplayName(), targets, e);
        }
        return metrics;
    }


    public void onTaskComplete() {
        try {
            AzureApiVersionStore.writeVersionMapToFile();
        } catch (Exception e) {
            LOGGER.error("[ACCOUNT={}] - Failed to write the versionsMap into the resource-version.json", account.getDisplayName());
        }
    }
}
