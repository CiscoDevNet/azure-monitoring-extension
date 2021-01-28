package com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons;

import com.appdynamics.extensions.azure.customnamespace.config.Account;
import com.appdynamics.extensions.azure.customnamespace.config.Configuration;
import com.appdynamics.extensions.azure.customnamespace.config.MetricConfig;
import com.appdynamics.extensions.azure.customnamespace.config.Service;
import com.appdynamics.extensions.azure.customnamespace.config.Stat;
import com.appdynamics.extensions.azure.customnamespace.utils.CommonUtilities;

import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.METRIC_PATH_SEPARATOR;

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.batch.BatchAccount;
import com.microsoft.azure.management.batchai.BatchAIWorkspace;
import com.microsoft.azure.management.cdn.CdnProfile;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.Gallery;
import com.microsoft.azure.management.compute.Snapshot;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.containerinstance.ContainerGroup;
import com.microsoft.azure.management.containerservice.ContainerService;
import com.microsoft.azure.management.containerservice.KubernetesCluster;
import com.microsoft.azure.management.cosmosdb.CosmosDBAccount;
import com.microsoft.azure.management.dns.DnsZone;
import com.microsoft.azure.management.eventhub.EventHubNamespace;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.monitor.ActionGroup;
import com.microsoft.azure.management.monitor.AutoscaleSetting;
import com.microsoft.azure.management.monitor.LocalizableString;
import com.microsoft.azure.management.monitor.MetricCollection;
import com.microsoft.azure.management.monitor.MetricDefinition;
import com.microsoft.azure.management.monitor.MetricValue;
import com.microsoft.azure.management.monitor.TimeSeriesElement;
import com.microsoft.azure.management.network.ApplicationGateway;
import com.microsoft.azure.management.network.ApplicationSecurityGroup;
import com.microsoft.azure.management.network.DdosProtectionPlan;
import com.microsoft.azure.management.network.ExpressRouteCircuit;
import com.microsoft.azure.management.network.ExpressRouteCrossConnection;
import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.network.LocalNetworkGateway;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkWatcher;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.RouteFilter;
import com.microsoft.azure.management.network.RouteTable;
import com.microsoft.azure.management.network.VirtualNetworkGateway;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.resources.GenericResource;
import com.microsoft.azure.management.search.SearchService;
import com.microsoft.azure.management.servicebus.ServiceBusNamespace;
import com.microsoft.azure.management.sql.SqlServer;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.trafficmanager.TrafficManagerProfile;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureMetricsCollector<T> implements Callable<List<Metric>> {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureMetricsCollector.class);
    private Azure azure;
    private Service monitoringService;
    private Account account;
    private MonitorContextConfiguration monitorContextConfiguration;
    private Configuration config;
    private String metricPrefix;
    private LongAdder requestCounter;
    private String loggingPrefix;

    private T service;
    private String matchedServiceName;

    public AzureMetricsCollector(Builder builder) {
        this.azure = builder.azure;
        this.monitoringService = builder.monitoringService;
        this.account = builder.account;
        this.monitorContextConfiguration = builder.monitorContextConfiguration;
        this.config = builder.config;
        this.metricPrefix = builder.metricPrefix;
        this.requestCounter = builder.requestCounter;

        this.loggingPrefix = "[ACCOUNT=" + this.account.getDisplayName() + ", SERVICE=" + this.monitoringService.getServiceName() + "]";
    }

    public void setService(T serviceQueryId) {
        this.service = serviceQueryId;
    }

    @Override
    public List<Metric> call() {
        return collectMetrics();
    }

    private List<Metric> collectMetrics() {
        List<Metric> metrics = Lists.newArrayList();
        try {
            LOGGER.debug("{} - Starting metrics collection for service", loggingPrefix);
            String serviceName = monitoringService.getServiceName();
            Stat stats = null;
            if (monitorContextConfiguration.getConfigYml().get("filterStats") == null || monitorContextConfiguration.getConfigYml().get("filterStats").equals(true))
                stats = ((Stat.Stats) monitorContextConfiguration.getMetricsXml()).getStats(serviceName);
            String resourceId = getFilteredResourceId(service);
            if (resourceId != null) {
                metricPrefix = metricPrefix + account.getDisplayName() + METRIC_PATH_SEPARATOR + serviceName + METRIC_PATH_SEPARATOR;
                for (MetricDefinition metricDefinition : azure.metricDefinitions().listByResource(resourceId)) {
                    MetricConfig matchedConfig = isMetricConfigured(stats, metricDefinition.name().value());
                    if (matchedConfig != null)
                        metrics.addAll(queryMetricDefinition(metricDefinition, matchedConfig));
                }
                LOGGER.debug("{} - Successfully collected all the metrics for service instance ||{}||", loggingPrefix, matchedServiceName);
            } else {
                LOGGER.warn("{} - Unabled to collect metrics for service.  Please review service configuration.", loggingPrefix);
            }
        } catch (Exception e) {
            LOGGER.error("{} - Exception while collecting metrics for service. Please review the exception for details.", loggingPrefix, e);
        } finally {
            LOGGER.debug("{} - Finished metrics collection for service", loggingPrefix);
        }
        return metrics;
    }

    private List<Metric> queryMetricDefinition(MetricDefinition metricDefinition, MetricConfig matchedConfig) {
        DateTime recordDateTime = DateTime.now();
        List<Metric> metrics = Lists.newArrayList();
        MetricCollection metricCollection = buildAndExecuteQuery(metricDefinition, recordDateTime, matchedConfig);
        requestCounter.increment();
        for (com.microsoft.azure.management.monitor.Metric azureMetric : metricCollection.metrics()) {
            for (TimeSeriesElement timeElement : azureMetric.timeseries()) {
                if (timeElement.data() != null && timeElement.data().size() > 0) {
                    MetricValue latestElement = getLatestTimeSeriesElement(timeElement);
                    String metricValue = fetchValueAsPerAggregation(latestElement, matchedConfig.getAggregationType());
                    if (!metricValue.equals("null")) {
                        Metric metric = new Metric(matchedConfig.getAlias(), metricValue, metricPrefix + matchedServiceName + METRIC_PATH_SEPARATOR + matchedConfig.getAlias());
                        metrics.add(metric);
                    }
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
                    .withInterval(Period.minutes(metricDefinition.metricAvailabilities().get(0).timeGrain().toStandardMinutes().getMinutes()));
            if (metricDefinition.isDimensionRequired()) {
                String defaultFilter = buildDimensionsFilterQuery(metricDefinition.dimensions());
                metricsQueryExecute.withOdataFilter(defaultFilter);
            }
            return metricsQueryExecute.execute();
        } catch (Exception e) {
            LOGGER.error("{} - Failed to execute the metric query", loggingPrefix, e);
        }
        return null;
    }

    private String buildDimensionsFilterQuery(List<LocalizableString> dimensions) {
        if (checkServiceDimensions(dimensions) == false)
            return null;
        StringBuilder filterQuery = new StringBuilder();
        filterQuery.append(dimensions.get(0).value() + " eq '*'");
        return filterQuery.toString();
    }


    private boolean checkServiceDimensions(List<LocalizableString> dimensions) {
        if (dimensions == null || dimensions.size() == 0)
            return false;
        return true;
    }

    private String getFilteredResourceId(T service) {
        String resourceId = null;
        List<String> serviceInstances = monitoringService.getServiceInstances();
        List<String> regions = monitoringService.getRegions();
        LOGGER.debug("{} - Looking to get instance resource id for with matching SDK type ||{}|| in configured instances ||{}|| and regions ||{}||", loggingPrefix, service.getClass().getSimpleName(), serviceInstances, regions);
        if (service instanceof ContainerGroup && checkServiceRegionPatternMatch(((ContainerGroup) service).name(), serviceInstances, ((ContainerGroup) service).region().label(), regions))
            resourceId = ((ContainerGroup) service).id();
        else if (service instanceof SqlServer && checkServiceRegionPatternMatch(((SqlServer) service).name(), serviceInstances, ((SqlServer) service).region().label(), regions))
            resourceId = (((SqlServer) service).id());
        else if (service instanceof StorageAccount && checkServiceRegionPatternMatch(((StorageAccount) service).name(), serviceInstances, ((StorageAccount) service).region().label(), regions))
            resourceId = (((StorageAccount) service).id());
        else if (service instanceof VirtualMachine && checkServiceRegionPatternMatch(((VirtualMachine) service).name(), serviceInstances, ((VirtualMachine) service).region().label(), regions)) {
            resourceId = ((VirtualMachine) service).id();
        } else if (service instanceof ActionGroup && checkServiceRegionPatternMatch(((ActionGroup) service).name(), serviceInstances, ((ActionGroup) service).region().label(), regions)) {
            resourceId = ((ActionGroup) service).id();
        } else if (service instanceof CosmosDBAccount && checkServiceRegionPatternMatch(((CosmosDBAccount) service).name(), serviceInstances, ((CosmosDBAccount) service).region().label(), regions)) {
            resourceId = ((CosmosDBAccount) service).id();
        } else if (service instanceof Disk && checkServiceRegionPatternMatch(((Disk) service).name(), serviceInstances, ((Disk) service).region().label(), regions)) {
            resourceId = ((Disk) service).id();
        } else if (service instanceof WebApp && checkServiceRegionPatternMatch(((WebApp) service).name(), serviceInstances, ((WebApp) service).region().label(), regions)) {
            resourceId = ((WebApp) service).id();
        } else if (service instanceof ApplicationGateway && checkServiceRegionPatternMatch(((ApplicationGateway) service).name(), serviceInstances, ((ApplicationGateway) service).region().label(), regions)) {
            resourceId = (((ApplicationGateway) service).id());
        } else if (service instanceof ApplicationSecurityGroup && checkServiceRegionPatternMatch(((ApplicationSecurityGroup) service).name(), serviceInstances, ((ApplicationSecurityGroup) service).region().label(), regions)) {
            resourceId = (((ApplicationSecurityGroup) service).id());
        } else if (service instanceof AutoscaleSetting && checkServiceRegionPatternMatch(((AutoscaleSetting) service).name(), serviceInstances, ((AutoscaleSetting) service).region().label(), regions)) {
            resourceId = (((AutoscaleSetting) service).id());
        } else if (service instanceof AvailabilitySet && checkServiceRegionPatternMatch(((AvailabilitySet) service).name(), serviceInstances, ((AvailabilitySet) service).region().label(), regions)) {
            resourceId = (((AvailabilitySet) service).id());
        } else if (service instanceof BatchAIWorkspace && checkServiceRegionPatternMatch(((BatchAIWorkspace) service).name(), serviceInstances, ((BatchAIWorkspace) service).region().label(), regions)) {
            resourceId = (((BatchAIWorkspace) service).id());
        } else if (service instanceof BatchAccount && checkServiceRegionPatternMatch(((BatchAccount) service).name(), serviceInstances, ((BatchAccount) service).region().label(), regions)) {
            resourceId = (((BatchAccount) service).id());
        } else if (service instanceof CdnProfile && checkServiceRegionPatternMatch(((CdnProfile) service).name(), serviceInstances, ((CdnProfile) service).region().label(), regions)) {
            resourceId = (((CdnProfile) service).id());
        } else if (service instanceof ContainerService && checkServiceRegionPatternMatch(((ContainerService) service).name(), serviceInstances, ((ContainerService) service).region().label(), regions)) {
            resourceId = (((ContainerService) service).id());
        } else if (service instanceof DdosProtectionPlan && checkServiceRegionPatternMatch(((DdosProtectionPlan) service).name(), serviceInstances, ((DdosProtectionPlan) service).region().label(), regions)) {
            resourceId = (((DdosProtectionPlan) service).id());
        }
//        else if (service instanceof Deployments && checkServiceRegionPatternMatch((( Deployments ) service).name(), serviceInstances, (( Deployments ) service).region().label(), regions)){
//            resourceId = ((( Deployments ) service).id());}
        else if (service instanceof DnsZone && checkServiceRegionPatternMatch(((DnsZone) service).name(), serviceInstances, ((DnsZone) service).region().label(), regions)) {
            resourceId = (((DnsZone) service).id());
        } else if (service instanceof EventHubNamespace && checkServiceRegionPatternMatch(((EventHubNamespace) service).name(), serviceInstances, ((EventHubNamespace) service).region().label(), regions)) {
            resourceId = (((EventHubNamespace) service).id());
        } else if (service instanceof ExpressRouteCircuit && checkServiceRegionPatternMatch(((ExpressRouteCircuit) service).name(), serviceInstances, ((ExpressRouteCircuit) service).region().label(), regions)) {
            resourceId = (((ExpressRouteCircuit) service).id());
        } else if (service instanceof ExpressRouteCrossConnection && checkServiceRegionPatternMatch(((ExpressRouteCrossConnection) service).name(), serviceInstances, ((ExpressRouteCrossConnection) service).region().label(), regions)) {
            resourceId = (((ExpressRouteCrossConnection) service).id());
        } else if (service instanceof Gallery && checkServiceRegionPatternMatch(((Gallery) service).name(), serviceInstances, ((Gallery) service).region().label(), regions)) {
            resourceId = (((Gallery) service).id());
        } else if (service instanceof GenericResource && checkServiceRegionPatternMatch(((GenericResource) service).name(), serviceInstances, ((GenericResource) service).region().label(), regions)) {
            resourceId = (((GenericResource) service).id());
        }
//        else if (service instanceof Identity && checkServiceRegionPatternMatch((( Identity ) service).name(), serviceInstances, (( Identity ) service).region().label(), regions)){
//            resourceId = ((( Identity ) service).id());}
        else if (service instanceof KubernetesCluster && checkServiceRegionPatternMatch(((KubernetesCluster) service).name(), serviceInstances, ((KubernetesCluster) service).region().label(), regions)) {
            resourceId = (((KubernetesCluster) service).id());
        } else if (service instanceof LoadBalancer && checkServiceRegionPatternMatch(((LoadBalancer) service).name(), serviceInstances, ((LoadBalancer) service).region().label(), regions)) {
            resourceId = (((LoadBalancer) service).id());
        } else if (service instanceof LocalNetworkGateway && checkServiceRegionPatternMatch(((LocalNetworkGateway) service).name(), serviceInstances, ((LocalNetworkGateway) service).region().label(), regions)) {
            resourceId = (((LocalNetworkGateway) service).id());
        }
//        else if (service instanceof ManagementLock && checkServiceRegionPatternMatch((( ManagementLock ) service).name(), serviceInstances, (( ManagementLock ) service).region().label(), regions)){
//            resourceId = ((( ManagementLock ) service).id());}
        else if (service instanceof NetworkInterface && checkServiceRegionPatternMatch(((NetworkInterface) service).name(), serviceInstances, ((NetworkInterface) service).region().label(), regions)) {
            resourceId = (((NetworkInterface) service).id());
        } else if (service instanceof NetworkSecurityGroup && checkServiceRegionPatternMatch(((NetworkSecurityGroup) service).name(), serviceInstances, ((NetworkSecurityGroup) service).region().label(), regions)) {
            resourceId = (((NetworkSecurityGroup) service).id());
        } else if (service instanceof NetworkWatcher && checkServiceRegionPatternMatch(((NetworkWatcher) service).name(), serviceInstances, ((NetworkWatcher) service).region().label(), regions)) {
            resourceId = (((NetworkWatcher) service).id());
        } else if (service instanceof Network && checkServiceRegionPatternMatch(((Network) service).name(), serviceInstances, ((Network) service).region().label(), regions)) {
            resourceId = (((Network) service).id());
        }
//        else if (service instanceof PolicyAssignment && checkServiceRegionPatternMatch((( PolicyAssignment ) service).name(), serviceInstances, (( PolicyAssignment ) service).region().label(), regions)){
//            resourceId = ((( PolicyAssignment ) service).id());}
        else if (service instanceof PublicIPAddress && checkServiceRegionPatternMatch(((PublicIPAddress) service).name(), serviceInstances, ((PublicIPAddress) service).region().label(), regions)) {
            resourceId = (((PublicIPAddress) service).id());
        } else if (service instanceof RedisCache && checkServiceRegionPatternMatch(((RedisCache) service).name(), serviceInstances, ((RedisCache) service).region().label(), regions)) {
            resourceId = (((RedisCache) service).id());
        }
//        else if (service instanceof ContainerRegistry && checkServiceRegionPatternMatch((( ContainerRegistry ) service).name(), serviceInstances, (( ContainerRegistry ) service).region().label(), regions)){
//            resourceId = ((( ContainerRegistry ) service).id());}
        else if (service instanceof RouteFilter && checkServiceRegionPatternMatch(((RouteFilter) service).name(), serviceInstances, ((RouteFilter) service).region().label(), regions)) {
            resourceId = (((RouteFilter) service).id());
        } else if (service instanceof RouteTable && checkServiceRegionPatternMatch(((RouteTable) service).name(), serviceInstances, ((RouteTable) service).region().label(), regions)) {
            resourceId = (((RouteTable) service).id());
        } else if (service instanceof SearchService && checkServiceRegionPatternMatch(((SearchService) service).name(), serviceInstances, ((SearchService) service).region().label(), regions)) {
            resourceId = (((SearchService) service).id());
        } else if (service instanceof ServiceBusNamespace && checkServiceRegionPatternMatch(((ServiceBusNamespace) service).name(), serviceInstances, ((ServiceBusNamespace) service).region().label(), regions)) {
            resourceId = (((ServiceBusNamespace) service).id());
        } else if (service instanceof Snapshot && checkServiceRegionPatternMatch(((Snapshot) service).name(), serviceInstances, ((Snapshot) service).region().label(), regions)) {
            resourceId = (((Snapshot) service).id());
        } else if (service instanceof TrafficManagerProfile && checkServiceRegionPatternMatch(((TrafficManagerProfile) service).name(), serviceInstances, ((TrafficManagerProfile) service).region().label(), regions)) {
            resourceId = (((TrafficManagerProfile) service).id());
        } else if (service instanceof Vault && checkServiceRegionPatternMatch(((Vault) service).name(), serviceInstances, ((Vault) service).region().label(), regions)) {
            resourceId = (((Vault) service).id());
        } else if (service instanceof VirtualMachineCustomImage && checkServiceRegionPatternMatch(((VirtualMachineCustomImage) service).name(), serviceInstances, ((VirtualMachineCustomImage) service).region().label(), regions)) {
            resourceId = (((VirtualMachineCustomImage) service).id());
        } else if (service instanceof VirtualMachineScaleSet && checkServiceRegionPatternMatch(((VirtualMachineScaleSet) service).name(), serviceInstances, ((VirtualMachineScaleSet) service).region().label(), regions)) {
            resourceId = (((VirtualMachineScaleSet) service).id());
        } else if (service instanceof VirtualNetworkGateway && checkServiceRegionPatternMatch(((VirtualNetworkGateway) service).name(), serviceInstances, ((VirtualNetworkGateway) service).region().label(), regions)) {
            resourceId = (((VirtualNetworkGateway) service).id());
        } else if (service instanceof AppServicePlan && checkServiceRegionPatternMatch(((AppServicePlan) service).name(), serviceInstances, ((AppServicePlan) service).region().label(), regions)) {
            resourceId = (((AppServicePlan) service).id());
        }
        if (resourceId == null) {
            LOGGER.debug("{} - Unable to get filtered resource id", loggingPrefix);            
        }
        return resourceId;
    }


    private MetricValue getLatestTimeSeriesElement(TimeSeriesElement timeSeriesElement) {
        List<MetricValue> datapoints = timeSeriesElement.data();
        for (int i = datapoints.size() - 1; i >= 0; i--) {
            MetricValue datapoint = datapoints.get(i);
            if (datapoint.average() != null || datapoint.count() != null || datapoint.maximum() != null || datapoint.minimum() != null || datapoint.total() != null) {
                return datapoints.get(i);
            }
        }
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

    private String fetchValueAsPerAggregation(MetricValue latestDataPoint, String aggreagationType) {
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
        LOGGER.debug("{} - Looking for match of service instance ||{}|| in instances ||{}|| and region ||{}|| in regions ||{}||", loggingPrefix, service, servicePatterns, region, regions);
        if (CommonUtilities.checkStringPatternMatch(service, servicePatterns) && CommonUtilities.checkStringPatternMatch(region, regions)) {
            LOGGER.debug("{} - Match found for service instance ||{}|| and region ||{}||", loggingPrefix, service, region);
            this.matchedServiceName = service;
            return true;
        }
        return false;
    }

    public static class Builder {
        private Azure azure;
        private Service monitoringService;
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
            this.monitoringService = service;
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

        public Builder withRequestCounter(LongAdder requestCounter) {
            this.requestCounter = requestCounter;
            return this;
        }

        public Builder withMetricPrefix(String metricPrefix) {
            this.metricPrefix = metricPrefix;
            return this;
        }

        public AzureMetricsCollector build() {
            return new AzureMetricsCollector(this);
        }

    }
}