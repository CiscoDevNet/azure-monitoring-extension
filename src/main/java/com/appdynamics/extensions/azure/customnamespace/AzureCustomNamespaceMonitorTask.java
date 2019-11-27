package com.appdynamics.extensions.azure.customnamespace;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.azure.customnamespace.azureAuthStore.AuthenticationFactory;
import com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons.AzureResourceGroupCollector;
import com.appdynamics.extensions.azure.customnamespace.config.Configuration;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.DISPLAY_NAME;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.RESOURCE_GROUPS;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.executorservice.MonitorThreadPoolExecutor;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.ResourceGroup;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private Map<String, ?> account;
    private Azure azure;
    private int heartBeat = 0;

    public AzureCustomNamespaceMonitorTask(MonitorContextConfiguration monitorContextConfiguration, Configuration config, MetricWriteHelper metricWriteHelper, Map<String, ?> account, String metricPrefix) {
        this.monitorContextConfiguration = monitorContextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.account = account;
        this.metricPrefix = metricPrefix;
        this.config = config;
    }

    public void run() {
        try {
            LOGGER.debug("Starting processing for the account {}", account.get("displayName"));
            azure = AuthenticationFactory.getAzure((Map<String, ?>) account.get("credentials"));
            if (azure == null)
                throw new Exception("Failed: built Azure object is null");
            else
                collectStatistics();
            LOGGER.debug("Completed processing for the account {}", account.get("displayName"));
        } catch (IOException IOe) {
            LOGGER.error("Error in Authentication of the Azure client", IOe);
        } catch (Exception e) {
            LOGGER.error("Exception while creating azure client", e);
        }
        onTaskComplete();
    }

    private void collectStatistics() {
        List<Metric> metrics = Lists.newArrayList();
        try {
            MonitorExecutorService executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(config.getConcurrencyConfig().getNoOfResourceGroupThreads()));
            List<FutureTask<List<Metric>>> resourceGroupFutureTask = buildFutureTasks(executorService);
            metrics = collectFutureMetrics(resourceGroupFutureTask);
            heartBeat = 1;
        } catch (Exception e) {
            LOGGER.error("Error while collecting stats for Account {}", account.get("displayName"), e);
        } finally {
            Metric heartbeat = new Metric("Heartbeat", String.valueOf(heartBeat), metricPrefix);
            metrics.add(heartbeat);
            metricWriteHelper.transformAndPrintMetrics(metrics);
        }
    }

    private List<FutureTask<List<Metric>>> buildFutureTasks(MonitorExecutorService executorService) {
        List<FutureTask<List<Metric>>> futureTasks = Lists.newArrayList();
        try {
            List<String> confResourceGroups = (List<String>) account.get(RESOURCE_GROUPS);
            List<ResourceGroup> filteredResourceGroups = filteredResourceGroups(azure.resourceGroups().list(), confResourceGroups);
            LOGGER.debug("Filtered resourceGroups are {}", filteredResourceGroups);
            for (ResourceGroup resourceGroup : filteredResourceGroups) {
                try {
                    AzureResourceGroupCollector accountTask = new AzureResourceGroupCollector(azure, account, monitorContextConfiguration, config, metricWriteHelper, resourceGroup.name(), metricPrefix);
                    FutureTask<List<Metric>> accountExecutorTask = new FutureTask(accountTask);
                    executorService.submit("AzureCustomNamespaceMonitorTask", accountExecutorTask);
                    futureTasks.add(accountExecutorTask);
                }catch (Exception e){
                    LOGGER.error("Error while collecting metrics for resourceGroup {}", resourceGroup.name(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception while querying for a resourceGroup of account {}", account.get(DISPLAY_NAME), e);
        } finally {
            return futureTasks;
        }
    }

    private List<Metric> collectFutureMetrics(List<FutureTask<List<Metric>>> tasks) {
        List<Metric> metrics = Lists.newArrayList();
        for (FutureTask<List<Metric>> task : tasks) {
            try {
                metrics.addAll(task.get(config.getConcurrencyConfig().getThreadTimeout(), TimeUnit.SECONDS));
            } catch (InterruptedException var6) {
                LOGGER.error("Task interrupted. ", var6);
            } catch (ExecutionException var7) {
                LOGGER.error("Task execution failed. ", var7);
            } catch (TimeoutException var8) {
                LOGGER.error("Task timed out. ", var8);
            }
        }
        return metrics;
    }

    private List<ResourceGroup> filteredResourceGroups(List<ResourceGroup> resourceGroups, List<String> confResourceGroups) {
        List<ResourceGroup> filteredResourceGroup = Lists.newArrayList();
        for (ResourceGroup resourceGroup : resourceGroups) {
            if (checkStringPatternMatch(resourceGroup.name(), confResourceGroups))
                filteredResourceGroup.add(resourceGroup);
            else
                LOGGER.debug("No match for resourceGroup {}, Excluding it", resourceGroup.name());
        }
        return filteredResourceGroup;
    }

    private boolean checkStringPatternMatch(String fullName, List<String> configPatterns) {
        for (String configPattern : configPatterns) {
            if (checkRegexMatch(fullName, configPattern)) {
                LOGGER.debug("Match found for name :" + fullName);
                return true;
            }
        }
        return false;
    }

    private boolean checkRegexMatch(String text, String pattern) {
        Pattern regexPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher regexMatcher = regexPattern.matcher(text);
        return regexMatcher.matches();
    }

    public void onTaskComplete() {
    }
}
