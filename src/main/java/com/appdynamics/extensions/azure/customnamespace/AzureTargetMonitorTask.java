package com.appdynamics.extensions.azure.customnamespace;

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
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.microsoft.aad.adal4j.AuthenticationResult;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.Callable;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureTargetMonitorTask implements Callable {
    Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureTargetMonitorTask.class);
    private Target target;
    private AuthenticationResult authTokenResult;
    private String metricPrefix;
    private String subscriptionId;
    private String SLASH = "/";
    private String RESOURCE_API_VERSION = "2019-01-01"; //Default api version
    private String TARGET_API_VERSION = "2019-01-01";

    public AzureTargetMonitorTask(Target target, AuthenticationResult authTokenResult, String metricPrefix, String subscriptionId) {
        this.target = target;
        this.authTokenResult = authTokenResult;
        this.metricPrefix = metricPrefix;
        this.subscriptionId = subscriptionId;
    }

    @Override
    public List<Metric> call() {
        List<Metric> metrics = Lists.newArrayList();
        try {
            metrics = targetMetricCollector();
        } catch (IOException IOe) {
            LOGGER.error("I/O exception occured while collecting target metrics", IOe);
        } catch (Exception e) {
            LOGGER.error("Error while collecting target metrics");
        }
        return metrics;
    }

    private List<Metric> targetMetricCollector() throws IOException {
        List<Metric> metrics = Lists.newArrayList();
//        https://www.baeldung.com/httpclient-connection-management#eviction
//        https://www.baeldung.com/httpclient-custom-http-header
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(new PoolingHttpClientConnectionManager()).build();
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
        RESOURCE_API_VERSION = AzureApiVersionStore.getAptApiVersion(httpClient, url,  RESOURCE_API_VERSION, resourceSubstrings[4], authTokenResult);
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
        return filterConfiguredResourceNames(resourceNames, target.getServiceInstances());
    }

    private void initTargetMetricsCollection(String resourceUrl, List<String> resourceNames, List<Metric> metrics, HttpClient client) throws IOException {
        for (String resourceName : resourceNames) {
            String responseBody = null;
            try {
                //TODO: put a check that if the resourceNames are configured then it should have the <MY-RESOURCE> in the resource string.
                resourceUrl = resourceUrl.replace("<MY-RESOURCE>", resourceName);
                String url = Constants.AZURE_MANAGEMENT + SUBSCRIPTION + SLASH + subscriptionId + resourceUrl + API_VERSION;
                String subUrl = collectiveMetricsNames(target.getMetrics());
                TARGET_API_VERSION = AzureApiVersionStore.getAptApiVersion(client, url, TARGET_API_VERSION, subUrl, resourceName, authTokenResult);
                url = url + TARGET_API_VERSION + subUrl;
                HttpGet request = new HttpGet(url + "&timespan=" + getTimespan(target.getTimeSpan()));
                request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());
                HttpResponse response = client.execute(request);
                responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject jsonObject = new JSONObject(responseBody);
                TargetJsonResponseParser jsonResponseParser = new TargetJsonResponseParser(metricPrefix, target);
                metrics.addAll(jsonResponseParser.parseJsonObject(jsonObject, resourceName));
            }catch (Exception e){
                LOGGER.error("Exception while target metric collection ", e.getMessage());
            }
        }
    }

    private String collectiveMetricsNames(List<MetricConfig> metricStats) {
        //If condition handles the case when no metrics are configured and will fetch all the available metrics
        if (metricStats.isEmpty())
            return "";
        StringBuilder metricsQuery = new StringBuilder();
        metricsQuery.append("&metricnames=");
        StringJoiner sj = new StringJoiner(",");

        for (MetricConfig metricStat : metricStats)
            sj.add(modifiedAttr(metricStat.getAttr()));
        metricsQuery.append(sj.toString()).append("&");

        return metricsQuery.toString();
    }

    private List<String> filterConfiguredResourceNames(List<String> resourceNames, List<String> targetResourceNames) {
        List<String> matchedResourceNames = Lists.newArrayList();
        for (String resourceName : resourceNames)
            if (CommonUtilities.checkStringPatternMatch(resourceName, targetResourceNames))
                matchedResourceNames.add(resourceName);

        return matchedResourceNames;
    }

    private String modifiedAttr(String attr) {
        return attr.replaceAll("\\s+", "%20");
    }

    private String getTimespan(String timeSpan) {
        return "PT" + timeSpan + "M";
    } // convert timeSpan to Azure timeGrain convention
}
