/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.azureTarget;

import com.appdynamics.extensions.azure.customnamespace.config.Account;
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

import org.apache.commons.httpclient.HttpStatus;
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
    private Account account;
    private AuthenticationResult authTokenResult;
    private Configuration configuration;
    private String subscriptionId;
    private String metricPrefix;
    private LongAdder requestCounter;

    private List<MetricConfig> metricConfigs;
    private Map<String, List<MetricConfig>> timeSpanMappedMetricConfig = Maps.newHashMap();
    private String SLASH = "/";
    private String RESOURCE_API_VERSION = "2019-01-01"; //Default api version
    private String ARM_API_VERSION = "2020-07-01"; //Need to get this from the config as it will change over time.
    private String METRICS_API_VERSION = "2018-01-01";
    private String loggingPrefix;

    public AzureTargetMonitorTask(Builder builder) {
        this.target = builder.target;
        this.account = builder.account;
        this.authTokenResult = builder.authTokenResult;
        this.configuration = builder.configuration;
        this.subscriptionId = builder.subscriptionId;
        this.metricPrefix = builder.metricPrefix;
        this.requestCounter = builder.requestCounter;

        this.loggingPrefix = "[ACCOUNT=" + this.account.getDisplayName() + ", TARGET=" + this.target.getDisplayName() + "]";
    }

    @Override
    public List<Metric> call() {
        List<Metric> metrics = Lists.newArrayList();
        try {
            metrics = targetMetricCollector();
        } catch (Exception e) {
            LOGGER.error("{} - Error while collecting target metrics", loggingPrefix, e);
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

        LOGGER.debug("{} - Starting metrics collection", loggingPrefix);
        List<String> matchedResourceGroupNames = queryAndMatchConfiguredResourceGroups(resourceSubstrings, httpClient);  

        if (matchedResourceGroupNames.isEmpty()) {
            LOGGER.warn("{} - No resources groups were found for target. Please check target configuration", loggingPrefix);
            return metrics;
        }
        for (String resourceGroup : matchedResourceGroupNames) {
            List<String> matchedResourceNames = queryAndMatchConfiguredResources(resourceGroup, resourceSubstrings, httpClient);
            if (matchedResourceNames.isEmpty()) {                
                LOGGER.warn("{} - No resources were found for target. Please check target configuration", loggingPrefix);
                break;                
            }   
            initTargetMetricsCollection((target.getResource()).replace("<MY-RESOURCE-GROUP>", resourceGroup), matchedResourceNames, metrics, httpClient);
            LOGGER.debug("{} - Completed metrics collection", loggingPrefix);         
        }        
        return metrics;
    }

    private List<String> queryAndMatchConfiguredResourceGroups(String[] resourceSubstrings, HttpClient httpClient) {
        List<String> resourceGroups = Lists.newArrayList();
        List<String> queriedResourceGroupNames = Lists.newArrayList();
        List<String> results = Lists.newArrayList();
        
        resourceGroups.addAll(target.getResourceGroups());

        try {
            if (!resourceSubstrings[2].equals("<MY-RESOURCE-GROUP>"))
                resourceGroups.add(resourceSubstrings[2]);
            
            String resourceGroupUrl = Constants.AZURE_MANAGEMENT + SUBSCRIPTION + SLASH + subscriptionId + SLASH + RESOURCE_GROUPS + API_VERSION + "2019-03-01";
            LOGGER.debug("{} - Resource Groups API request: ||{}||", loggingPrefix, resourceGroupUrl);
            
            HttpGet request = new HttpGet(resourceGroupUrl);
            request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());
            
            HttpResponse response = httpClient.execute(request);
            LOGGER.debug("{} - Resource Groups API response status code ||{}||", loggingPrefix, response.getStatusLine().getStatusCode());
            requestCounter.increment();

            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            LOGGER.trace("{} - Resource Groups API response ||{}||", loggingPrefix, responseBody);
            
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                try {
                    JSONArray jsonResourcesArray = new JSONObject(responseBody).getJSONArray("value");
                    for (int i = 0; i < jsonResourcesArray.length(); i++) {
                        JSONObject jsonResource = jsonResourcesArray.getJSONObject(i);
                        queriedResourceGroupNames.add(jsonResource.getString("name"));
                    }
                    results = TargetUtils.filterConfiguredResourceNames(queriedResourceGroupNames, resourceGroups);    
                } catch (Exception e) {
                    LOGGER.error("{} - Exception parsing resource group response for target.  Please review response and exception for details. Response: ||{}||", loggingPrefix, responseBody, e);
                }                
            } else {
                LOGGER.warn("{} - Unable to get resource groups from API for target.  Please review response for details. Response: ||{}||", loggingPrefix, responseBody);
            }                    
        } catch (Exception e) {
            LOGGER.error("{} - Error while collecting the Resource Groups", loggingPrefix, e);
        }        
        return results;
    }

    private List<String> queryAndMatchConfiguredResources(String resourceGroup, String[] resourceSubstrings, HttpClient httpClient) {
        List<String> resourceNames = Lists.newArrayList();
        List<String> results = Lists.newArrayList();
        String responseBody = null;
        try {
            String resourceProviderUrl = Constants.AZURE_MANAGEMENT + SUBSCRIPTION + SLASH + subscriptionId + "/providers/" + resourceSubstrings[4] + API_VERSION + ARM_API_VERSION;
        
            RESOURCE_API_VERSION = AzureApiVersionStore.getDefaultApiVersion(httpClient, resourceProviderUrl, resourceSubstrings[5], authTokenResult, loggingPrefix);
            requestCounter.increment();

            if (RESOURCE_API_VERSION == null) {
                LOGGER.warn("{} - A default API version was not found for target resource type.  Please check if the target string is valid.", loggingPrefix);
                return results;
            }
            
            String resourceType = resourceSubstrings[4] + "/" + resourceSubstrings[5];
            String subUrl = SLASH + RESOURCE_GROUPS + SLASH + resourceGroup + "/providers/" + resourceType + API_VERSION;

            String url = Constants.AZURE_MANAGEMENT + SUBSCRIPTION + SLASH + subscriptionId + subUrl + RESOURCE_API_VERSION;                           
            LOGGER.debug("{} - Resource Type |{}|| API request: ||{}||", loggingPrefix, resourceType, url);

            HttpGet request = new HttpGet(url);
            request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());

            HttpResponse response = httpClient.execute(request);
            LOGGER.debug("{} - Resource Type ||{}|| API response status code: ||{}||", loggingPrefix, resourceType, response.getStatusLine().getStatusCode());
            requestCounter.increment();

            responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");            
            LOGGER.trace("{} - Resource Type ||{}|| API response ||{}||", loggingPrefix, resourceType, responseBody);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                try {
            JSONArray jsonResourcesArray = new JSONObject(responseBody).getJSONArray("value");
                    for (int i = 0; i < jsonResourcesArray.length(); i++) {
                        JSONObject jsonResource = jsonResourcesArray.getJSONObject(i);
                        resourceNames.add(jsonResource.getString("name"));
                    }
                    results = TargetUtils.filterConfiguredResourceNames(resourceNames, target.getServiceInstances());
                }
                catch (Exception e) {
                    LOGGER.error("{} - Error while parsing resource type response for target. Please review the response and exception for details. Response: ||{}||", loggingPrefix, responseBody, e);
                }               
            }
            
        } catch (Exception e) {
            LOGGER.error("{} - Error while gathering resource types for target. Please check if the target string is valid and review the exception for details.", loggingPrefix, e);

        }

        return results;
    }

    private void initTargetMetricsCollection(String resourceUrl, List<String> resourceNames, List<Metric> metrics, HttpClient client) {
        for (String resourceName : resourceNames) {
            try {
                resourceUrl = resourceUrl.replace("<MY-RESOURCE>", resourceName);
                String url = Constants.AZURE_MANAGEMENT + SUBSCRIPTION + SLASH + subscriptionId + resourceUrl + API_VERSION;

                metricConfigs = target.getMetrics();
                
                metricConfigsProcessor(resourceName, client, url);
                
                if (metricConfigs.isEmpty()) {
                    LOGGER.warn("{} - Unable to continue processing for target.  No valid metrics configurations available. Please check if target configuration is valid.");
                    return;
                }

                MonitorExecutorService executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(timeSpanMappedMetricConfig.size()));
                
                List<FutureTask<List<Metric>>> futureTasks = Lists.newArrayList();
                for (Map.Entry<String, List<MetricConfig>> entry : timeSpanMappedMetricConfig.entrySet()) {
                    try {
                        TimegrainTargetCollectorTask targetTask = new TimegrainTargetCollectorTask.Builder()
                                        .withTarget(target)
                                        .withAccount(account)
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
                        LOGGER.error("{} - Error while building targetTask for resource ||{}|| using time grain ||{}||", loggingPrefix, resourceName, entry.getKey(), e);
                    }
                }

                metrics.addAll(CommonUtilities.collectFutureMetrics(futureTasks, 100, "AzureTargetMonitorTask"));

            } catch (Exception e) {
                LOGGER.error("{} - Exception in initTargetMetricsCollection() for resource ||{}||", loggingPrefix, resourceName, e);
            }
        }
    }

    private void metricConfigsProcessor(String resourceName, HttpClient httpClient, String url) throws IOException {
        //Truncating to <= 20 as per error {"code":"BadRequest","message":"Requested metrics count: 59 bigger than allowed max: 20"}
        int count = 0;
        List<MetricConfig> filteredConfigs = Lists.newArrayList();

        List<MetricConfig> actualMetricConfigs = getActualMetricsDefinitions(resourceName, httpClient, url);
        if (actualMetricConfigs.isEmpty()) {
            LOGGER.warn("{} - No metric definitions found for target. Please review logs for details.", loggingPrefix);
            metricConfigs = filteredConfigs;
            return;
        }
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
    private List<MetricConfig> getActualMetricsDefinitions(String resourceName, HttpClient httpClient, String url) throws IOException {
        List<MetricConfig> actualMetricConfigs = Lists.newArrayList();
        url = url.replace("metrics", "metricDefinitions");
        LOGGER.debug("{} - resource ||{}|| metrics definition API request: ||{}||||{}||", loggingPrefix, resourceName, url, METRICS_API_VERSION);

        HttpGet request = new HttpGet(url + METRICS_API_VERSION);        
        request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());
        HttpResponse response = httpClient.execute(request);
        LOGGER.debug("{} - resource ||{}|| metrics definition API response status code: ||{}||", loggingPrefix, resourceName, response.getStatusLine().getStatusCode());
        requestCounter.increment();

        String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
        LOGGER.trace("{} - resource ||{}|| metrics definition API response: ||{}||", loggingPrefix, resourceName, responseBody);

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            try {
                JSONObject jsonObject = new JSONObject(responseBody);
                TargetUtils.scanJsonResponseforMetricConfigs(jsonObject, actualMetricConfigs);
            } catch(Exception e) {
                LOGGER.error("{} - Exception parsing metric definitions response for target. Please review response and exception for details. Response: ||{}||", loggingPrefix, responseBody, e);
            }
            
        } else {
            LOGGER.warn("{} - Unable to get metric definitions for target. Please review response for details. Response: ||{}||", loggingPrefix, responseBody);
        } 
        
        return actualMetricConfigs;
    }

    public static class Builder {
        private Target target;
        private Account account;
        private AuthenticationResult authTokenResult;
        private Configuration configuration;
        private String subscriptionId;
        private String metricPrefix;
        private LongAdder requestCounter;

        public Builder withTarget(Target target) {
            this.target = target;
            return this;
        }

        public Builder withAccount(Account account) {
            this.account = account;
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

        public Builder withSubscriptionId(String subscriptionId ) {
            this.subscriptionId = subscriptionId ;
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