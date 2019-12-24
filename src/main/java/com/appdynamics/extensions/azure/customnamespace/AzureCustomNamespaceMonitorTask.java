package com.appdynamics.extensions.azure.customnamespace;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.azure.customnamespace.azureAuthStore.AuthenticationFactory;
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
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureCustomNamespaceMonitorTask implements AMonitorTaskRunnable {
    Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureCustomNamespaceMonitorTask.class);
    private String metricPrefix;
    private MonitorContextConfiguration monitorContextConfiguration;
    private Configuration config;
    private MetricWriteHelper metricWriteHelper;
    private Account account;
    private Azure azure;
    private AuthenticationResult authTokenResult;
    private BigInteger heartBeat = BigInteger.valueOf(0);

    public AzureCustomNamespaceMonitorTask(MonitorContextConfiguration monitorContextConfiguration, Configuration config, MetricWriteHelper metricWriteHelper, Account account, String metricPrefix) {
        this.monitorContextConfiguration = monitorContextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.account = account;
        this.metricPrefix = metricPrefix;
        this.config = config;
    }

    public void run() {
        try {
            LOGGER.debug("Starting processing for the account {}", account.getDisplayName());
            azure = AuthenticationFactory.getAzure(account.getCredentials());
            authTokenResult = AuthenticationFactory.getAccessTokenFromUserCredentials();
            if (azure == null)
                throw new Exception("Failed: built Azure object is null");
            else
                collectStatistics();
            LOGGER.debug("Completed processing for the account {}", account.getDisplayName());
        } catch (IOException IOe) {
            LOGGER.error("Error in Authentication of the Azure client", IOe);
        } catch (Exception e) {
            LOGGER.error("Exception while creating azure client", e);
        }
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
            heartBeat = BigInteger.valueOf(1);
        } catch (Exception e) {
            LOGGER.error("Error while collecting stats for Account {}", account.getDisplayName(), e);
        } finally {
            Metric heartbeat = new Metric("Heartbeat", String.valueOf(heartBeat), metricPrefix);
            metrics.add(heartbeat);
            metricWriteHelper.transformAndPrintMetrics(metrics);
        }
    }

    private List<FutureTask<List<Metric>>> buildFutureTasks(MonitorExecutorService executorService) {
        List<FutureTask<List<Metric>>> futureTasks = Lists.newArrayList();
        try {
            List<Service> services = account.getServices();
            for (Service service : services) {
                try {
                    LOGGER.debug("Started processing the service {}", service.getServiceName());
                    AzureServiceCollector serviceCollectorTask = new AzureServiceCollector(azure, account, monitorContextConfiguration, config, metricWriteHelper, service, metricPrefix);
                    FutureTask<List<Metric>> accountExecutorTask = new FutureTask(serviceCollectorTask);
                    executorService.submit("AzureCustomNamespaceMonitorTask", accountExecutorTask);
                    futureTasks.add(accountExecutorTask);
                } catch (Exception e) {
                    LOGGER.error("Error while collecting metrics for resourceGroup {}", service.getServiceName(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception while querying for a resourceGroup of account {}", account.getDisplayName(), e);
        } finally {
            return futureTasks;
        }
    }

    private List<FutureTask<List<Metric>>> buildFutureTargetTasks(MonitorExecutorService executorService, List<Target> targets) {
        List<FutureTask<List<Metric>>> futureTasks = Lists.newArrayList();
        String targetName = null;
        try {
            for (Target target : targets) {
                try {
                    targetName = target.getDisplayName();
                    AzureTargetMonitorTask targetTask = new AzureTargetMonitorTask(target, authTokenResult, metricPrefix, account.getCredentials().getSubscriptionId());
                    FutureTask<List<Metric>> targetExecutorTask = new FutureTask(targetTask);
                    executorService.submit("AzureTargetMonitorTask", targetExecutorTask);
                    futureTasks.add(targetExecutorTask);
                } catch (Exception e) {
                    LOGGER.error("Error while collecting metrics for Target {}", targetName, e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception while querying for a target {} for account {}", targetName, e);
        } finally {
            return futureTasks;
        }
    }

    private List<Metric> initTargetMetricsCollection(List<Target> targets) {
        List<Metric> metrics = Lists.newArrayList();
        try {
            MonitorExecutorService executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(targets.size()));
            List<FutureTask<List<Metric>>> targetFutureTask = buildFutureTargetTasks(executorService, targets);
            metrics = CommonUtilities.collectFutureMetrics(targetFutureTask, config.getConcurrencyConfig().getThreadTimeout(), "AzureCustomNamespaceMonitorTasl");
        } catch (Exception e) {
            LOGGER.error("Exception occurred while collecting target metrics", e);
        }
        return metrics;
    }

    public void onTaskComplete() {
        try {
            AzureApiVersionStore.writeVersionMapToFile();
        } catch (Exception e) {
            LOGGER.error("Failed to write the versionsMap into the resource-version.json");
        }
    }
}
