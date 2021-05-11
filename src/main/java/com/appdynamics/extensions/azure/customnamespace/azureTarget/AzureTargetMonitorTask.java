/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.azureTarget;

import com.appdynamics.extensions.azure.customnamespace.config.Configuration;
import com.appdynamics.extensions.azure.customnamespace.config.MetricConfig;
import com.appdynamics.extensions.azure.customnamespace.config.Target;
import com.appdynamics.extensions.azure.customnamespace.utils.AzureApiVersionStore;
import com.appdynamics.extensions.azure.customnamespace.utils.CommonUtilities;
import com.appdynamics.extensions.azure.customnamespace.utils.Constants;

import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.API_VERSION;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.AUTHORIZATION;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.BEARER;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.RESOURCE_GROUPS;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.SUBSCRIPTION;

import com.appdynamics.extensions.conf.modules.HttpClientModule;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.executorservice.MonitorThreadPoolExecutor;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.microsoft.aad.adal4j.AuthenticationResult;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

public class AzureTargetMonitorTask implements Callable {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureTargetMonitorTask.class);
    private Target target;
    private AuthenticationResult authTokenResult;
    private Configuration configuration;
    private String subscriptionId;
    private String metricPrefix;
    private LongAdder requestCounter;

    private List<MetricConfig> metricConfigs;
    private Map<String, List<MetricConfig>> timeSpanMappedMetricConfig = Maps.newHashMap();
    private String SLASH = "/";
    private String RESOURCE_API_VERSION = "2019-01-01"; //Default api version

    public AzureTargetMonitorTask(Builder builder) {
        this.target = builder.target;
        this.authTokenResult = builder.authTokenResult;
        this.configuration = builder.configuration;
        this.subscriptionId = builder.subscriptionId;
        this.metricPrefix = builder.metricPrefix;
        this.requestCounter = builder.requestCounter;
    }

    @Override
    public List<Metric> call() {
        List<Metric> metrics = Lists.newArrayList();
        try {
            metrics = targetMetricCollector();
        } catch (Exception e) {
            LOGGER.error("Error while collecting server metrics");
        }
        return metrics;
    }

    private List<Metric> targetMetricCollector() {
        List<Metric> metrics = Lists.newArrayList();
        HttpClientModule httpClientModule = new HttpClientModule();

        Map<String, List<Map<String, String>>> config = TargetUtils.httpClientConfigTransformer(configuration, target);
        httpClientModule.initHttpClient(config);
        CloseableHttpClient httpClient = httpClientModule.getHttpClient();
        String resource = target.getResource();
        String[] resourceSubstrings = resource.split(SLASH);
        List<String> matchedResourceGroupNames = queryAndMatchConfiguredResourceGroups(resourceSubstrings, httpClient);
        for (String resourceGroup : matchedResourceGroupNames) {
            List<String> matchedResourceNames = queryAndMatchConfiguredResources(resourceGroup, resourceSubstrings, httpClient);
            initTargetMetricsCollection((target.getResource()).replace("<MY-RESOURCE-GROUP>", resourceGroup), matchedResourceNames, metrics, httpClient);
        }
        return metrics;
    }

    private List<String> queryAndMatchConfiguredResourceGroups(String[] resourceSubstrings, HttpClient httpClient) {
        List<String> resourceGroups = Lists.newArrayList();
        List<String> queriedResourceGroupNames = Lists.newArrayList();
        resourceGroups.addAll(target.getResourceGroups());
        try {
            if (!resourceSubstrings[2].equals("<MY-RESOURCE-GROUP>"))
                resourceGroups.add(resourceSubstrings[2]);
            String resourceGroupUrl = Constants.AZURE_MANAGEMENT + SUBSCRIPTION + SLASH + subscriptionId + SLASH + RESOURCE_GROUPS + API_VERSION + "2019-03-01";
            HttpGet request = new HttpGet(resourceGroupUrl);
            request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());
            HttpResponse response = httpClient.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            requestCounter.increment();
            JSONArray jsonResourcesArray = new JSONObject(responseBody).getJSONArray("value");
            for (int i = 0; i < jsonResourcesArray.length(); i++) {
                JSONObject jsonResource = jsonResourcesArray.getJSONObject(i);
                queriedResourceGroupNames.add(jsonResource.getString("name"));
            }
        } catch (Exception e) {
            LOGGER.error("Error while collecting the resourceGroups", e);
        }
        return TargetUtils.filterConfiguredResourceNames(queriedResourceGroupNames, resourceGroups);
    }

    private List<String> queryAndMatchConfiguredResources(String resourceGroup, String[] resourceSubstrings, HttpClient httpClient) {
        String subUrl = SLASH + RESOURCE_GROUPS + SLASH + resourceGroup + "/providers/" + resourceSubstrings[4] + "/" + resourceSubstrings[5] + API_VERSION;

        String url = Constants.AZURE_MANAGEMENT + SUBSCRIPTION + SLASH + subscriptionId + subUrl;
        RESOURCE_API_VERSION = AzureApiVersionStore.getAptApiVersion(httpClient, url, RESOURCE_API_VERSION, resourceSubstrings[4], authTokenResult);
        url = url + RESOURCE_API_VERSION;
        List<String> resourceNames = Lists.newArrayList();
        String responseBody = null;
        try {
            HttpGet request = new HttpGet(url);
            request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());
            HttpResponse response1 = httpClient.execute(request);
            responseBody = EntityUtils.toString(response1.getEntity(), "UTF-8");
            JSONArray jsonResourcesArray = new JSONObject(responseBody).getJSONArray("value");
            requestCounter.increment();
            for (int i = 0; i < jsonResourcesArray.length(); i++) {
                JSONObject jsonResource = jsonResourcesArray.getJSONObject(i);
                resourceNames.add(jsonResource.getString("name"));
            }
        } catch (Exception e) {
            LOGGER.error("Error while collecting metrics from the resource", e);

        }
        return TargetUtils.filterConfiguredResourceNames(resourceNames, target.getServiceInstances());
    }

    private void initTargetMetricsCollection(String resourceUrl, List<String> resourceNames, List<Metric> metrics, HttpClient client) {
        MonitorExecutorService executorService = null;
        try {
            executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(timeSpanMappedMetricConfig.size()));
            for (String resourceName : resourceNames) {
                resourceUrl = resourceUrl.replace("<MY-RESOURCE>", resourceName);
                String url = Constants.AZURE_MANAGEMENT + SUBSCRIPTION + SLASH + subscriptionId + resourceUrl + API_VERSION;
                metricConfigs = target.getMetrics();
                metricConfigsProcessor(client, url);
                List<FutureTask<List<Metric>>> futureTasks = Lists.newArrayList();
                for (Map.Entry<String, List<MetricConfig>> entry : timeSpanMappedMetricConfig.entrySet()) {
                    try {
                        TimegrainTargetCollectorTask targetTask = new TimegrainTargetCollectorTask.Builder()
                                .withTarget(target)
                                .withAuthenticationResult(authTokenResult)
                                .withClient(client)
                                .withUrl(url)
                                .withGrainEntry(entry)
                                .withResourceName(resourceName)
                                .withMetricPrefix(metricPrefix)
                                .withRequestCounter(requestCounter)
                                .build();
                        FutureTask<List<Metric>> targetExecutorTask = new FutureTask(targetTask);
                        executorService.submit("TimegrainTargetCollectorTask", targetExecutorTask);
                        futureTasks.add(targetExecutorTask);
                    } catch (Exception e) {
                        LOGGER.error("Error while collecting metrics for Grain {}", entry.getKey(), e);
                    }
                }
                metrics.addAll(CommonUtilities.collectFutureMetrics(futureTasks, 100, "AzureTargetMonitorTask"));
            }
        } catch (Exception e) {
            LOGGER.error("Exception while server metric collection ", e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }

    private void metricConfigsProcessor(HttpClient httpClient, String url) throws IOException {
        //Truncating to <= 20 as per error {"code":"BadRequest","message":"Requested metrics count: 59 bigger than allowed max: 20"}
        int count = 0;
        List<MetricConfig> actualMetricConfigs = getActualMetrics(httpClient, url);

        //collect all metrics if the metrics stats are missing
        if (metricConfigs.isEmpty()) {
            for (MetricConfig metricConfig : actualMetricConfigs) {
                count++;
                updateTimespanMap(metricConfig);
                if (count >= 20) {
                    metricConfigs = actualMetricConfigs.subList(0, 20);
                    return;
                }
            }
        }
        List<MetricConfig> filteredConfigs = Lists.newArrayList();
        for (MetricConfig metricStat : metricConfigs) {
            Boolean isValidMetricConfig = TargetUtils.matchedAndModifiedAttr(metricStat, actualMetricConfigs);
            if (isValidMetricConfig != false)
                updateTimespanMap(metricStat);
            filteredConfigs.add(metricStat);
        }
        metricConfigs = filteredConfigs;
    }

    private void updateTimespanMap(MetricConfig metricConfig) {
        if (timeSpanMappedMetricConfig.get(metricConfig.getTimeSpan()) == null) {
            List<MetricConfig> configList = Lists.newArrayList();
            configList.add(metricConfig);
            timeSpanMappedMetricConfig.put(metricConfig.getTimeSpan(), configList);
        } else {
            List<MetricConfig> configList = timeSpanMappedMetricConfig.get(metricConfig.getTimeSpan());
            configList.add(metricConfig);
        }
    }

    //get actual metrics from metric definition
    private List<MetricConfig> getActualMetrics(HttpClient httpClient, String url) throws IOException {
        List<MetricConfig> actualMetricConfigs = Lists.newArrayList();
        url = url.replace("metrics", "metricDefinitions");
        HttpGet request = new HttpGet(url + "2018-01-01");
        request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());
        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
        LOGGER.debug("Target Response from endpoint: " + responseBody);
        JSONObject jsonObject = new JSONObject(responseBody);
        TargetUtils.scanJsonResponseforMetricConfigs(jsonObject, actualMetricConfigs);
        return actualMetricConfigs;
    }

    public static class Builder {
        private Target target;
        private AuthenticationResult authTokenResult;
        private Configuration configuration;
        private String subscriptionId;
        private String metricPrefix;
        private LongAdder requestCounter;

        public Builder withTarget(Target target) {
            this.target = target;
            return this;
        }

        public Builder withAuthenticationResult(AuthenticationResult authTokenResult) {
            this.authTokenResult = authTokenResult;
            return this;
        }

        public Builder withConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder withSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
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

        public AzureTargetMonitorTask build() {
            return new AzureTargetMonitorTask(this);
        }
    }

}