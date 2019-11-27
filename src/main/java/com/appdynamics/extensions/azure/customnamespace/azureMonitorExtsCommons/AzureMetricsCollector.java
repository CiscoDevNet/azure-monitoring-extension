package com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.azure.customnamespace.config.Configuration;
import com.appdynamics.extensions.azure.customnamespace.config.MetricConfig;
import com.appdynamics.extensions.azure.customnamespace.config.Stat;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.DISPLAY_NAME;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.SERVICE;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.containerinstance.ContainerGroup;
import com.microsoft.azure.management.cosmosdb.CosmosDBAccount;
import com.microsoft.azure.management.monitor.ActionGroup;
import com.microsoft.azure.management.monitor.LocalizableString;
import com.microsoft.azure.management.monitor.MetricCollection;
import com.microsoft.azure.management.monitor.MetricDefinition;
import com.microsoft.azure.management.monitor.MetricValue;
import com.microsoft.azure.management.monitor.TimeSeriesElement;
import com.microsoft.azure.management.sql.SqlServer;
import com.microsoft.azure.management.storage.StorageAccount;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureMetricsCollector<T> extends TaskBuilder {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger("AzureMetricsCollector.class");
    private T resourceGroup;
    private String service;

    public AzureMetricsCollector(Azure azure, Map<String, ?> account, MonitorContextConfiguration monitorContextConfiguration, Configuration config, MetricWriteHelper metricWriteHelper, String metricPrefix, T cont) {
        super(azure, account, monitorContextConfiguration, config, metricWriteHelper, metricPrefix);
        this.resourceGroup = cont;
    }

    @Override
    public List<Metric> call() {
        return collectMetrics();
    }

    private List<Metric> collectMetrics() {
        List<Metric> metrics = Lists.newArrayList();
        try {
            String service = (String) account.get(SERVICE);
            Stat stats = null;
            if (monitorContextConfiguration.getConfigYml().get("filterStats").equals(true))
                stats = ((Stat.Stats) monitorContextConfiguration.getMetricsXml()).getStats(service);
            String resourceId = getFilteredResourceId(resourceGroup);
            if (resourceId != null) {
                metricPrefix = metricPrefix + account.get(DISPLAY_NAME) + METRIC_PATH_SEPARATOR + service + METRIC_PATH_SEPARATOR;
                LOGGER.debug("Starting metrics collection for displayname {}. service {}", account.get(DISPLAY_NAME), service);
                for (MetricDefinition metricDefinition : azure.metricDefinitions().listByResource(resourceId)) {
                    MetricConfig matchedConfig = isMetricConfigured(stats, metricDefinition.name().value());
                    metrics.addAll(queryMetricDefinintion(metricDefinition, matchedConfig));
                }
                LOGGER.debug("Successfully collected all the metrics for displayname {}, service {}", account.get(DISPLAY_NAME), service);
            } else {
                LOGGER.debug("service did not match for Resource Group {}", resourceGroup.toString());
            }
        } catch (Exception e) {
            LOGGER.debug("Exception in AzureMetricsCollector, failed while collect metrics ", e);
        } finally {
            return metrics;
        }
    }

    private List<Metric> queryMetricDefinintion(MetricDefinition metricDefinition, MetricConfig matchedConfig) {
        DateTime recordDateTime = DateTime.now();
        List<Metric> metrics = Lists.newArrayList();
        MetricCollection metricCollection = buildAndExecuteQuery(metricDefinition, recordDateTime, matchedConfig);
        for (com.microsoft.azure.management.monitor.Metric azureMetric : metricCollection.metrics()) {
            for (TimeSeriesElement timeElement : azureMetric.timeseries()) {
                MetricValue latestElement = getLatestTimeSeriesElement(timeElement);
                String metricValue = fetchValueAsPerAggregatoin(latestElement, matchedConfig.getAggregationType());
                if (!metricValue.equals("null")) {
                    //TODO: put region/resourcegrp in metricss
                    Metric metric = new Metric(matchedConfig.getAlias(), metricValue, metricPrefix + service + METRIC_PATH_SEPARATOR + matchedConfig.getAlias());
                    metrics.add(metric);
                }
            }
        }
        return metrics;
    }

    //Sample dimension values for storage account https://docs.microsoft.com/en-us/azure/storage/common/storage-metrics-in-azure-monitor
    private MetricCollection buildAndExecuteQuery(MetricDefinition metricDefinition, DateTime recordDateTime, MetricConfig matchedConfig) {
        try {
            MetricDefinition.MetricsQueryDefinition metricsQueryExecute = (MetricDefinition.MetricsQueryDefinition) metricDefinition.defineQuery()
                    .startingFrom(recordDateTime.minusMinutes(config.getMetricsTimeRange().getStartTimeInMinsBeforeNow()))
                    .endsBefore(recordDateTime.minusMinutes(config.getMetricsTimeRange().getEndTimeInMinsBeforeNow()))
                    .withAggregation(matchedConfig.getAggregationType())
                    .withInterval(Period.minutes(metricDefinition.metricAvailabilities().get(0).timeGrain().toStandardMinutes().getMinutes())); //TODO: pre-configure this time interval as per the requirement
            //TODO: short-circuited the dimensions and fetch all
            if (metricDefinition.isDimensionRequired()) {
                String defaultFilter = buildDimensionsFilterQuery(metricDefinition.dimensions());
                metricsQueryExecute.withOdataFilter(defaultFilter);
            }
            return metricsQueryExecute.execute();
        } catch (Exception e) {
            LOGGER.error("Failed to execute the metric query");
        }
        return null;
    }
//"DatabaseResourceId eq '202f9a13-69d0-4a6e-a006-e7612275771f'"

    private boolean checkServiceDimensions(List<LocalizableString> dimensions) {
        if (dimensions == null || dimensions.size() == 0)
            return false;
        return true;
    }

    //      Return all time series of C where A = a1 and B = b1 or b2&lt;br&gt;
//    * **$filter=A eq ‘a1’
//            and B eq ‘b1’ or B eq ‘b2’
//            and C eq ‘*   ’
    private String buildDimensionsFilterQuery(List<LocalizableString> dimensions) {
//        sanitizeDimensions(dimensions);
        if (checkServiceDimensions(dimensions) == false)
            return null;
        StringBuilder filterQuery = new StringBuilder();
        filterQuery.append(dimensions.get(0).value() + " eq '*'");
        return filterQuery.toString();
    }

    private String getFilteredResourceId(T resourceGroup) {
        String resourceId = null;
        List<String> serviceInstances = (List<String>) account.get("serviceInstances");
        List<String> regions = (List<String>) account.get("regions");
        if (resourceGroup instanceof ContainerGroup && checkServiceRegionPatternMatch(((ContainerGroup) resourceGroup).name(), serviceInstances, ((ContainerGroup) resourceGroup).region().label(), regions))
            resourceId = ((ContainerGroup) resourceGroup).id();
        else if (resourceGroup instanceof SqlServer && checkServiceRegionPatternMatch(((SqlServer) resourceGroup).name(), serviceInstances, ((SqlServer) resourceGroup).region().label(), regions))
            resourceId = (((SqlServer) resourceGroup).id());
        else if (resourceGroup instanceof StorageAccount && checkServiceRegionPatternMatch(((StorageAccount) resourceGroup).name(), serviceInstances, ((StorageAccount) resourceGroup).region().label(), regions))
            resourceId = (((StorageAccount) resourceGroup).id());
        else if (resourceGroup instanceof VirtualMachine && checkServiceRegionPatternMatch(((VirtualMachine) resourceGroup).name(), serviceInstances, ((VirtualMachine) resourceGroup).region().label(), regions)) {
            resourceId = ((VirtualMachine) resourceGroup).id();
        } else if (resourceGroup instanceof ActionGroup && checkServiceRegionPatternMatch(((ActionGroup) resourceGroup).name(), serviceInstances, ((ActionGroup) resourceGroup).region().label(), regions)) {
            resourceId = ((ActionGroup) resourceGroup).id();
        } else if (resourceGroup instanceof CosmosDBAccount && checkServiceRegionPatternMatch(((CosmosDBAccount) resourceGroup).name(), serviceInstances, ((CosmosDBAccount) resourceGroup).region().label(), regions)) {
            resourceId = ((CosmosDBAccount) resourceGroup).id();
        } else if (resourceGroup instanceof Disk && checkServiceRegionPatternMatch(((Disk) resourceGroup).name(), serviceInstances, ((Disk) resourceGroup).region().label(), regions)) {
            resourceId = ((Disk) resourceGroup).id();
        }
        if(resourceId == null){
            LOGGER.info("No match for the service being monitored {}", service);
        }
        return resourceId;
    }

    private MetricValue getLatestTimeSeriesElement(TimeSeriesElement timeSeriesElement) {
        List<MetricValue> datapoints = timeSeriesElement.data();
        return datapoints.get(datapoints.size() - 1);
    }

    private MetricConfig isMetricConfigured(Stat stats, String metricDefinitionName) {
        if (stats == null)
            return dummyMetricConfig(metricDefinitionName);
        for (MetricConfig config : stats.getMetricConfig()) {
            if (config.getAttr().equals(metricDefinitionName))
                return config;
        }
        return null;
    }

    private MetricConfig dummyMetricConfig(String metricName) {
        MetricConfig dummy = new MetricConfig();
        dummy.setAttr(metricName);
        dummy.setAlias(metricName);
        dummy.setAggregationType("AVERAGE");
        return dummy;
    }

    private String fetchValueAsPerAggregatoin(MetricValue latestDataPoint, String aggreagationType) {
        switch (aggreagationType) {
            case "AVERAGE":
                return latestDataPoint.average() != null ? Double.toString(latestDataPoint.average()) : "null";
            case "MINIMUM":
                return latestDataPoint.minimum() != null ? Double.toString(latestDataPoint.minimum()) : "null";
            case "MAXIMUM":
                return latestDataPoint.maximum() != null ? Double.toString(latestDataPoint.maximum()) : "null";
            case "TOTAL":
                return latestDataPoint.total() != null ? Double.toString(latestDataPoint.total()) : "null";
            case "COUNT":
                return latestDataPoint.count() != null ? Double.toString(latestDataPoint.count()) : "null";
            default:
                return "null";
        }
    }


    private boolean checkServiceRegionPatternMatch(String service, List<String> servicePatterns, String region, List<String> regions) {
        if (checkStringPatternMatch(service, servicePatterns) && checkStringPatternMatch(region, regions)) {
            LOGGER.debug("Match found for name :" + service);
            this.service = service;
            return true;
        }
        return false;
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

}
