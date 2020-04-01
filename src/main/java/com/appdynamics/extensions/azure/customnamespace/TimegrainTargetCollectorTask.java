package com.appdynamics.extensions.azure.customnamespace;

import com.appdynamics.extensions.azure.customnamespace.config.MetricConfig;
import com.appdynamics.extensions.azure.customnamespace.config.Target;
import com.appdynamics.extensions.azure.customnamespace.utils.AzureApiVersionStore;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.AUTHORIZATION;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.BEARER;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.microsoft.aad.adal4j.AuthenticationResult;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.Callable;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class TimegrainTargetCollectorTask implements Callable {
    Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureTargetMonitorTask.class);
    private Map.Entry<String, List<MetricConfig>> grainEntry;
    private HttpClient client;
    private String url;
    private String resourceName;
    private AuthenticationResult authTokenResult;
    private String metricPrefix;
    private Target server;
    private String TARGET_API_VERSION = "2019-01-01";

    public TimegrainTargetCollectorTask(Map.Entry<String, List<MetricConfig>> grainEntry, HttpClient client, String url, String resourceName, AuthenticationResult authenticationResult, Target server, String metricPrefix) {
        this.grainEntry = grainEntry;
        this.client = client;
        this.url = url;
        this.resourceName = resourceName;
        this.authTokenResult = authenticationResult;
        this.metricPrefix = metricPrefix;
        this.server = server;
    }

    @Override
    public Object call() throws Exception {
        List<Metric> metrics = Lists.newArrayList();
        try {
            metricsCollector(metrics);
        } catch (Exception e) {
            LOGGER.error("Exception while granular metric collections", e);
        }
        return metrics;
    }

    private void metricsCollector(List<Metric> metrics) throws IOException {
        String subUrl = apiMetricNamesBuilder(grainEntry.getValue());
        TARGET_API_VERSION = AzureApiVersionStore.getAptApiVersion(client, url, TARGET_API_VERSION, resourceName, authTokenResult);
        HttpGet request = new HttpGet((url + TARGET_API_VERSION + subUrl + "&timespan=" + grainEntry.getKey()).replaceAll("\\s+", "%20"));
        request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());
        HttpResponse response = client.execute(request);
        if (response.getStatusLine().toString().contains("200 OK")) {
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONObject jsonObject = new JSONObject(responseBody);
            TargetJsonResponseParser jsonResponseParser = new TargetJsonResponseParser(metricPrefix, server);
            metrics.addAll(jsonResponseParser.parseJsonObject(jsonObject, resourceName, grainEntry.getValue()));
        }
    }


    private String apiMetricNamesBuilder(List<MetricConfig> metricConfigs) {
        StringBuilder metricsQuery = new StringBuilder();
        metricsQuery.append("&metricnames=");
        StringJoiner sj = new StringJoiner(",");
        for (MetricConfig metricConfig : metricConfigs) {
            sj.add(metricConfig.getAttr());
        }
        metricsQuery.append(sj.toString()).append("&");
        return metricsQuery.toString();
    }

}
