package com.appdynamics.extensions.azure.customnamespace;

import com.appdynamics.extensions.azure.customnamespace.config.Target;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.METRIC_PATH_SEPARATOR;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class TargetJsonResponseParser {
    Logger LOGGER = ExtensionsLoggerFactory.getLogger(TargetJsonResponseParser.class);
    public String metricPrefix;
    public Target target;
    public TargetJsonResponseParser(String metricPrefix, Target target){
        this.metricPrefix = metricPrefix;
        this.target = target;
    }
    public List<Metric> parseJsonObject(JSONObject jsonObject, String resourceName) {
        List<Metric> metrics = Lists.newArrayList();
        metricPrefix = metricPrefix + target.getDisplayName() + METRIC_PATH_SEPARATOR;
        try {
            JSONArray objectList = jsonObject.getJSONArray("value");
            int length = objectList.length();
            while (length-- > 0) {
                JSONObject currObj = objectList.getJSONObject(length);
                String metricName = currObj.getJSONObject("name").getString("value");
                JSONObject jsonValue = currObj.getJSONArray("timeseries").getJSONObject(0).getJSONArray("data").getJSONObject(Integer.parseInt((String) target.getTimeSpan()) - 1);
                if (jsonValue.keySet().contains("total")) {
                    Double value = (Double) jsonValue.get("total");
                    Metric metric = new Metric(metricName, Double.toString(value), metricPrefix + resourceName + METRIC_PATH_SEPARATOR + metricName);
                    metrics.add(metric);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while parsing the JSONArray", e);
        } finally {
            return metrics;
        }
    }
}
