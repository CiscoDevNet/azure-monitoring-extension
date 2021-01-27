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
import com.appdynamics.extensions.azure.customnamespace.config.MetricConfig;
import com.appdynamics.extensions.azure.customnamespace.config.Target;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.METRIC_PATH_SEPARATOR;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;

public class TargetJsonResponseParser {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(TargetJsonResponseParser.class);
    public String metricPrefix;
    public Target target;
    public Account account;
    private String loggingPrefix;

    public TargetJsonResponseParser(String metricPrefix, Account account, Target target) {
        this.metricPrefix = metricPrefix;
        this.target = target;
        this.account = account;
        
        this.loggingPrefix = "[ACCOUNT=" + this.account.getDisplayName() + ", TARGET=" + this.target.getDisplayName() + "]";
    }

    public List<Metric> parseJsonObject(JSONObject jsonObject, String resourceName, List<MetricConfig> metricConfigs) {
        List<Metric> metrics = Lists.newArrayList();
        metricPrefix = metricPrefix + account.getDisplayName() + METRIC_PATH_SEPARATOR + target.getDisplayName() + METRIC_PATH_SEPARATOR;
        try {
            JSONArray objectList = jsonObject.getJSONArray("value");
            int length = objectList.length();
            while (length-- > 0) {
                JSONObject currObj = objectList.getJSONObject(length);
                String metricName = currObj.getJSONObject("name").getString("value");
                if (!currObj.getJSONArray("timeseries").isEmpty()) {
                    MetricConfig thisMetricConfig = TargetUtils.matchMetricConfig(metricName, metricConfigs);
                    JSONObject metricData = currObj.getJSONArray("timeseries").getJSONObject(0).getJSONArray("data").getJSONObject(TargetUtils.getTimegrain(thisMetricConfig.getTimeSpan()) - 1);
                    String configAggregationType = thisMetricConfig.getAggregationType().toLowerCase();
                    if (metricData.keySet().contains(configAggregationType)) {
                        Double value = (Double) metricData.get(thisMetricConfig.getAggregationType().toLowerCase());
                        Metric metric = new Metric(metricName, Double.toString(value), metricPrefix + resourceName + METRIC_PATH_SEPARATOR + metricName);
                        metrics.add(metric);
                    } else
                        LOGGER.debug("{} - metric ||{}|| aggregation type ||{}|| doesn't match the config aggregation type of ||{}|| for resource: ||{}||", loggingPrefix, metricName, metricData, configAggregationType, resourceName);
                }
                else {
                    LOGGER.debug("{} - metric ||{}|| timeseries data was empty for resource: ||{}|| ", loggingPrefix, metricName, resourceName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("{} - Error while parsing metric data for resource: ||{}||", loggingPrefix, resourceName, e);
        } 
        return metrics;
    }
}
