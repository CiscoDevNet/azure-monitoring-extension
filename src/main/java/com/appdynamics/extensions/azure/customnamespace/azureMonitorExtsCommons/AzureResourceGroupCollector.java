package com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.azure.customnamespace.AzureNamespaceGroupFactory.NameSpaceGroupFactory;
import com.appdynamics.extensions.azure.customnamespace.config.Configuration;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.DISPLAY_NAME;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.SERVICE;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.executorservice.MonitorThreadPoolExecutor;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.microsoft.azure.management.Azure;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureResourceGroupCollector<T> extends TaskBuilder {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger("AzureResourceGroupCollector.class");
    private String resourceGroup;

    public AzureResourceGroupCollector(Azure azure, Map<String, ?> account, MonitorContextConfiguration monitorContextConfiguration, Configuration config, MetricWriteHelper metricWriteHelper, String resourceGroup, String metricPrefix) {
        super(azure, account, monitorContextConfiguration, config, metricWriteHelper, metricPrefix);
        this.resourceGroup = resourceGroup;
    }

    public List<Metric> call() {
        return collectMetrics();
    }

    private List<Metric> collectMetrics() {
        try {
            List<T> resourceGroupList = NameSpaceGroupFactory.getNamespaceGroup(azure, (String) account.get(SERVICE), resourceGroup);
            if (resourceGroupList.size() > 0) {
                MonitorExecutorService executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(config.getConcurrencyConfig().getNoOfMetricsCollectorThreads()));
                List<FutureTask<List<Metric>>> tasks = buildFutureTasks(executorService, resourceGroupList);
                return collectFutureMetrics(tasks);
            }
        }catch (Exception e){
            LOGGER.info("Exeception caught in AzureResourceGroupCollector", e);
        }
        return Lists.newArrayList();
    }

    private List<FutureTask<List<Metric>>> buildFutureTasks(MonitorExecutorService executorService, List<T> resourceGroupList) {
        List<FutureTask<List<Metric>>> tasks = Lists.newArrayList();
        for (T resourceGroup : resourceGroupList) {
            try {
                LOGGER.debug("starting AzureMetricCollector task for {}", resourceGroup.toString());
                AzureMetricsCollector<T> accountTask = new AzureMetricsCollector(azure, account, monitorContextConfiguration, config, metricWriteHelper, metricPrefix, resourceGroup);
                FutureTask<List<Metric>> accountExecutorTask = new FutureTask(accountTask);
                executorService.submit("AzureResourceGroupCollector", accountExecutorTask);
                tasks.add(accountExecutorTask);
            }catch (Exception e){
                LOGGER.info("Exception while collecting metrics for resourceGroup {}, service {}, accountname {}", resourceGroup.toString(), account.get(SERVICE), account.get(DISPLAY_NAME));
            }
        }
        return tasks;
    }

    private List<Metric> collectFutureMetrics(List<FutureTask<List<Metric>>> tasks) {
        List<Metric> metrics = Lists.newArrayList();
        for (FutureTask<List<Metric>> task : tasks) {
            try {
            metrics = task.get(this.config.getConcurrencyConfig().getThreadTimeout(), TimeUnit.SECONDS);
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
}





