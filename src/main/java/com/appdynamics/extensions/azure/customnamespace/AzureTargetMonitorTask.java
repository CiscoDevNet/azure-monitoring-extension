package com.appdynamics.extensions.azure.customnamespace;

import com.appdynamics.extensions.azure.customnamespace.utils.Constants;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.METRIC_PATH_SEPARATOR;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.microsoft.aad.adal4j.AuthenticationResult;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

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
public class AzureTargetMonitorTask implements Callable {

    private Map<String, ?> target;
    private AuthenticationResult authTokenResult;
    private String metricPrefix;
    private String subscriptionId;

    public AzureTargetMonitorTask(Map<String, ?> target, AuthenticationResult authTokenResult, String metricPrefix, String subscriptionId) {
        this.target = target;
        this.authTokenResult = authTokenResult;
        this.metricPrefix = metricPrefix;
        this.subscriptionId = subscriptionId;
    }

    @Override
    public List<Metric> call() throws Exception {
        List<Metric> metrics = Lists.newArrayList();
        initTargetMetricsCollection();
        return metrics;
    }

    private List<Metric> initTargetMetricsCollection() throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        String url = Constants.AZURE_MANAGEMENT + "subscriptions/" + subscriptionId + target.get("resource") + "?api-version=" + target.get("apiVersion") + collectiveMetricsNames((List<Map<String, ?>>)target.get("metrics")) ;
        HttpGet request = new HttpGet(url + "timespan=" + getTimespan(target.get("timeSpan").toString()));
        request.addHeader("Authorization", "Bearer " + authTokenResult.getAccessToken());
        HttpResponse response = client.execute(request);

        String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
        JSONObject jsonObject = new JSONObject(responseBody);

        return parseJsonObject(jsonObject);
    }

    private String collectiveMetricsNames(List<Map<String, ?>> metricStats){
        StringBuilder metricsQuery = new StringBuilder();
        metricsQuery.append("&metricnames=");

        StringJoiner sj = new StringJoiner(",");

        for(Map<String, ?> metricStat : metricStats)
            sj.add(modifiedAttr((String)metricStat.get("attr")));
        metricsQuery.append(sj.toString()).append("&");

        return metricsQuery.toString();
    }

    private List<Metric> parseJsonObject(JSONObject jsonObject){
        List<Metric> metrics = Lists.newArrayList();
        metricPrefix = metricPrefix + target.get(Constants.DISPLAY_NAME) + METRIC_PATH_SEPARATOR;
        JSONArray objectList = jsonObject.getJSONArray("value");
        int length = objectList.length();
        while (length-- > 0){
            JSONObject currObj = objectList.getJSONObject(length);
            String jsonName = currObj.getJSONObject("name").getString("value");
            Double value = (Double) currObj.getJSONArray("timeseries").getJSONObject(0).getJSONArray("data").getJSONObject(Integer.parseInt((String) target.get("timeSpan")) - 1).get("total");
            Metric metric = new Metric(jsonName, Double.toString(value), metricPrefix + jsonName);
            metrics.add(metric);
        }
        return metrics;
    }

    private String modifiedAttr(String attr){
        return attr.replaceAll("\\s+","%20");
    }

    private String getTimespan(String timeSpan){
        return "PT" + timeSpan + "M";
    } // convert timeSpan to Azure timeGrain convention
}
