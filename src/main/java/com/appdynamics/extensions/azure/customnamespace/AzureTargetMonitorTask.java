package com.appdynamics.extensions.azure.customnamespace;

import com.appdynamics.extensions.azure.customnamespace.config.Connection;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.microsoft.aad.adal4j.AuthenticationResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureTargetMonitorTask implements Callable {
    Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureTargetMonitorTask.class);
    private Target server;
    private AuthenticationResult authTokenResult;
    private Connection connection;
    private String metricPrefix;
    private String subscriptionId;
    private String SLASH = "/";
    private String RESOURCE_API_VERSION = "2019-01-01"; //Default api version

    private List<MetricConfig> metricConfigs;
    private Map<String, List<MetricConfig>> timeSpanMappedMetricConfig = Maps.newHashMap();

    public AzureTargetMonitorTask(Target server, Connection connection, AuthenticationResult authTokenResult, String metricPrefix, String subscriptionId) {
        this.server = server;
        this.authTokenResult = authTokenResult;
        this.metricPrefix = metricPrefix;
        this.subscriptionId = subscriptionId;
        this.connection = connection;
    }

    @Override
    public List<Metric> call() {
        List<Metric> metrics = Lists.newArrayList();
        try {
            metrics = targetMetricCollector();
        } catch (IOException IOe) {
            LOGGER.error("I/O exception occured while collecting server metrics", IOe);
        } catch (Exception e) {
            LOGGER.error("Error while collecting server metrics");
        }
        return metrics;
    }

    private List<Metric> targetMetricCollector() throws IOException {
        List<Metric> metrics = Lists.newArrayList();
//        https://www.baeldung.com/httpclient-connection-management#eviction
//        https://www.baeldung.com/httpclient-custom-http-header

//        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(new PoolingHttpClientConnectionManager()).build();
        HttpClientModule httpClientModule = new HttpClientModule();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> connectionMap = mapper.convertValue(connection, Map.class);
        Map<String, String> server1 = mapper.convertValue(server, Map.class);
        List<Map<String, String>> serversList = Lists.newArrayList();

        serversList.add(server1);
        Map<String, Object> config = Maps.newHashMap();
        config.put("servers", serversList);
        config.put("connection", connectionMap);

        httpClientModule.initHttpClient(config);
        CloseableHttpClient httpClient = httpClientModule.getHttpClient();
        String resource = server.getResource();
        String[] resourceSubstrings = resource.split(SLASH);
        List<String> matchedResourceGroupNames = queryAndMatchConfiguredResourceGroups(resourceSubstrings, httpClient);
        for (String resourceGroup : matchedResourceGroupNames) {
            List<String> matchedResourceNames = queryAndMatchConfiguredResources(resourceGroup, resourceSubstrings, httpClient);
            initTargetMetricsCollection((server.getResource()).replace("<MY-RESOURCE-GROUP>", resourceGroup), matchedResourceNames, metrics, httpClient);
        }
        return metrics;
    }

    private List<String> queryAndMatchConfiguredResourceGroups(String[] resourceSubstrings, HttpClient httpClient) {
        List<String> resourceGroups = Lists.newArrayList();
        List<String> queriedResourceGroupNames = Lists.newArrayList();
        resourceGroups.addAll(server.getResourceGroups());
        try {
            if (!resourceSubstrings[2].equals("<MY-RESOURCE-GROUP>"))
                resourceGroups.add(resourceSubstrings[2]);
            String resourceGroupUrl = Constants.AZURE_MANAGEMENT + SUBSCRIPTION + SLASH + subscriptionId + SLASH + RESOURCE_GROUPS + API_VERSION + "2019-03-01";
            HttpGet request = new HttpGet(resourceGroupUrl);
            request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());
            HttpResponse response = httpClient.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONArray jsonResourcesArray = new JSONObject(responseBody).getJSONArray("value");
            for (int i = 0; i < jsonResourcesArray.length(); i++) {
                JSONObject jsonResource = jsonResourcesArray.getJSONObject(i);
                queriedResourceGroupNames.add(jsonResource.getString("name"));
            }
        } catch (Exception e) {
            LOGGER.error("Error while collecting the resourceGroups", e);
        }
        return filterConfiguredResourceNames(queriedResourceGroupNames, resourceGroups);
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
            for (int i = 0; i < jsonResourcesArray.length(); i++) {
                JSONObject jsonResource = jsonResourcesArray.getJSONObject(i);
                resourceNames.add(jsonResource.getString("name"));
            }
        } catch (Exception e) {
            LOGGER.error("Error while collecting metrics from the resource", e);

        }
        return filterConfiguredResourceNames(resourceNames, server.getServiceInstances());
    }

    private void initTargetMetricsCollection(String resourceUrl, List<String> resourceNames, List<Metric> metrics, HttpClient client) {
        for (String resourceName : resourceNames) {
            try {
                //TODO: put a check that if the resourceNames are configured then it should have the <MY-RESOURCE> in the resource string.
                resourceUrl = resourceUrl.replace("<MY-RESOURCE>", resourceName);
                String url = Constants.AZURE_MANAGEMENT + SUBSCRIPTION + SLASH + subscriptionId + resourceUrl + API_VERSION;
                metricConfigs = server.getMetrics();
                metricConfigsProcessor(client, url);
                MonitorExecutorService executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(timeSpanMappedMetricConfig.size()));
                List<FutureTask<List<Metric>>> futureTasks = Lists.newArrayList();
                for (Map.Entry<String, List<MetricConfig>> entry : timeSpanMappedMetricConfig.entrySet()) {
                    try {

                        TimegrainTargetCollectorTask targetTask = new TimegrainTargetCollectorTask(entry, client, url, resourceName, authTokenResult, server, metricPrefix);
                        FutureTask<List<Metric>> targetExecutorTask = new FutureTask(targetTask);
                        executorService.submit("TimegrainTargetCollectorTask", targetExecutorTask);
                        futureTasks.add(targetExecutorTask);
                    } catch (Exception e) {
                        LOGGER.error("Error while collecting metrics for Grain {}", entry.getKey(), e);
                    }
                }
                metrics.addAll(CommonUtilities.collectFutureMetrics(futureTasks, 100, "AzureTargetMonitorTask"));
            } catch (Exception e) {
                LOGGER.error("Exception while server metric collection ", e.getMessage());
            }
        }
    }

    private void metricConfigsProcessor(HttpClient httpClient, String url) throws IOException {
        //If condition handles the case when no metrics are configured and will fetch all the available metrics
//        {"code":"BadRequest","message":"Requested metrics count: 59 bigger than allowed max: 20"}
        //Truncating to <= 20
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
            Boolean isValidMetricConfig = matchedAndModifiedAttr(metricStat, actualMetricConfigs);
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
        JSONObject jsonObject = new JSONObject(responseBody);
        scanJsonResponseforMetrics(jsonObject, actualMetricConfigs);
        return actualMetricConfigs;
    }

    private void scanJsonResponseforMetrics(JSONObject jsonObject, List<MetricConfig> actualMetricConfigs) {
//jsonObject.getJSONArray("value").get(0).get("name").get("value")
        //TODO: check for the NPE and important attributes for use
        JSONArray metricDefinitions = jsonObject.getJSONArray("value");
        for (int i = 0; i < metricDefinitions.length(); i++) {
            MetricConfig newConfig = new MetricConfig();
            String name = ((JSONObject) ((JSONObject) metricDefinitions.get(i)).get("name")).getString("value");
            newConfig.setAttr(name);
            newConfig.setAlias(name);
            newConfig.setAggregationType(((JSONObject) metricDefinitions.get(i)).getString("primaryAggregationType"));
            String timespan = ((JSONObject) ((JSONArray) (((JSONObject) metricDefinitions.get(i)).get("metricAvailabilities"))).get(0)).getString("timeGrain");
            newConfig.setTimeSpan(timespan);
            actualMetricConfigs.add(newConfig);
        }
    }

    private List<String> filterConfiguredResourceNames(List<String> resourceNames, List<String> targetResourceNames) {
        List<String> matchedResourceNames = Lists.newArrayList();
        for (String resourceName : resourceNames)
            if (CommonUtilities.checkStringPatternMatch(resourceName, targetResourceNames))
                matchedResourceNames.add(resourceName);

        return matchedResourceNames;
    }

    private Boolean matchedAndModifiedAttr(MetricConfig metricStat, List<MetricConfig> actualMetricConfigs) {
        String attr = metricStat.getAttr().toLowerCase();
        for (MetricConfig currMetricConfig : actualMetricConfigs) {
            if (currMetricConfig.getAttr().toLowerCase().equals(attr)) {
                metricStat.setTimeSpan(currMetricConfig.getTimeSpan());
                if(metricStat.getAggregationType() != null || metricStat.getAggregationType() != "")
                    metricStat.setAggregationType(currMetricConfig.getAggregationType());
                return true;
            }
        }
        return false;
    }

}