package com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.azure.customnamespace.config.Configuration;
import com.appdynamics.extensions.azure.customnamespace.config.MetricConfig;
import com.appdynamics.extensions.azure.customnamespace.config.Stat;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.DISPLAY_NAME;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.INTERVAL;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.SERVICE;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.containerinstance.ContainerGroup;
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

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureMetricsCollector<T> extends TaskBuilder {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger("AzureMetricsCollector.class");
    private T resourceGroup;

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
            Stat stats = ((Stat.Stats) monitorContextConfiguration.getMetricsXml()).getStats(service);
            String resourceId = getResourceId(resourceGroup);
            metricPrefix = metricPrefix + account.get(DISPLAY_NAME) + METRIC_PATH_SEPARATOR + service + METRIC_PATH_SEPARATOR;
            LOGGER.debug("Starting metrics collection for displayname {}. service {}", account.get(DISPLAY_NAME), service);
            for (MetricDefinition metricDefinition : azure.metricDefinitions().listByResource(resourceId)) {
                MetricConfig matchedConfig = isMetricConfigured(stats, metricDefinition.name().value());
                if (matchedConfig != null)
                    metrics.addAll(queryMetricDefinintion(metricDefinition, matchedConfig));
            }
            LOGGER.debug("Successfully collected all the metrics for displayname {}, service {}", account.get(DISPLAY_NAME), service);
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
                    Metric metric = new Metric(matchedConfig.getAlias(), metricValue, metricPrefix + azureMetric.type() + matchedConfig.getAlias());
                    metrics.add(metric);
                }
            }
        }
        return metrics;
    }
//Sample dimension values for storage account https://docs.microsoft.com/en-us/azure/storage/common/storage-metrics-in-azure-monitor
    private MetricCollection buildAndExecuteQuery(MetricDefinition metricDefinition, DateTime recordDateTime, MetricConfig matchedConfig) {

        MetricDefinition.MetricsQueryDefinition metricsQueryExecute = (MetricDefinition.MetricsQueryDefinition) metricDefinition.defineQuery()
                .startingFrom(recordDateTime.minusMinutes(config.getMetricsTimeRange().getStartTimeInMinsBeforeNow()))
                .endsBefore(recordDateTime.minusMinutes(config.getMetricsTimeRange().getEndTimeInMinsBeforeNow()))
                .withAggregation(matchedConfig.getAggregationType())
                .withInterval(Period.minutes((Integer) account.get(INTERVAL))); //TODO: pre-configure this time interval as per the requirement
//        if (checkServiceDimensions((Map<String, List<String>>) account.get("dimensions")))
//            metricsQueryExecute.withOdataFilter(buildDimensionsFilterQuery((Map<String, List<String>>) account.get("dimensions"))); //http://azure.github.io/ref-docs/java/com/microsoft/azure/management/monitor/MetricDefinition.MetricsQueryDefinitionStages.WithMetricsQueryExecute.html
        String defaultFilter = null;
        metricsQueryExecute.withOdataFilter(defaultFilter);
        return metricsQueryExecute.execute();
    }
//"DatabaseResourceId eq '202f9a13-69d0-4a6e-a006-e7612275771f'"

    private boolean checkServiceDimensions(Map<String, List<String>> dimensions) {
        if (dimensions == null || dimensions.values() == null)
            return false;
        return true;
    }

    //      Return all time series of C where A = a1 and B = b1 or b2&lt;br&gt;
//    * **$filter=A eq ‘a1’
//            and B eq ‘b1’ or B eq ‘b2’
//            and C eq ‘*   ’
    private String buildDimensionsFilterQuery(Map<String, List<String>> dimensions) {
//        sanitizeDimensions(dimensions);
        StringBuilder filterQuery = new StringBuilder();
        for (Map.Entry<String, List<String>> dimension : dimensions.entrySet()) {
            if(dimension.getValue() != null) {
                if (filterQuery.length() != 0)
                    filterQuery.append(" and");
                List<String> values = dimension.getValue();
                boolean orFlag = false;
                for (String value : values) {
                    if (!orFlag)
                        orFlag = true;
                    else
                        filterQuery.append(" or ");

                    filterQuery.append(dimension.getKey() + " eq '" + value + "'");
                }
            }
        }
        return filterQuery.toString();
    }

    private String getResourceId(T resourceGroup) {
        String resourceId = null;
        if (resourceGroup instanceof ContainerGroup)
            resourceId = ((ContainerGroup) resourceGroup).id();
        else if (resourceGroup instanceof SqlServer)
            resourceId = (((SqlServer) resourceGroup).id());
        else if (resourceGroup instanceof StorageAccount)
            resourceId = (((StorageAccount) resourceGroup).id());
        else if (resourceGroup instanceof VirtualMachine)
            resourceId = ((VirtualMachine) resourceGroup).id();
        return resourceId;
    }

    private MetricValue getLatestTimeSeriesElement(TimeSeriesElement timeSeriesElement) {
        List<MetricValue> datapoints = timeSeriesElement.data();
        return datapoints.get(datapoints.size() - 1);
    }

    private MetricConfig isMetricConfigured(Stat stats, String metricDefinitionName) {
        for (MetricConfig config : stats.getMetricConfig()) {
            if (config.getAttr().equals(metricDefinitionName))
                return config;
        }
        return null;
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

}
