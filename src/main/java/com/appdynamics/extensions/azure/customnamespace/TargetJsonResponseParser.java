package com.appdynamics.extensions.azure.customnamespace;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class TargetJsonResponseParser {
    Logger LOGGER = ExtensionsLoggerFactory.getLogger(TargetJsonResponseParser.class);
    public String metricPrefix;
    public Target server;
    private static final Pattern p = Pattern.compile("[^\\d]*[\\d]+[^\\d]+([\\d]+)");

    public TargetJsonResponseParser(String metricPrefix, Target server) {
        this.metricPrefix = metricPrefix;
        this.server = server;
    }

    public List<Metric> parseJsonObject(JSONObject jsonObject, String resourceName, List<MetricConfig> metricConfigs) {
        List<Metric> metrics = Lists.newArrayList();
        metricPrefix = metricPrefix + server.getDisplayName() + METRIC_PATH_SEPARATOR;
        try {
            JSONArray objectList = jsonObject.getJSONArray("value");
            int length = objectList.length();
            while (length-- > 0) {
                JSONObject currObj = objectList.getJSONObject(length);
                if (!currObj.getJSONArray("timeseries").isEmpty()) {
                    String metricName = currObj.getJSONObject("name").getString("value");
                    //find out the metricName in the metricconfigs
                    MetricConfig thisMetricConfig = matchMetricConfig(metricName, metricConfigs);

                    JSONObject jsonValue = currObj.getJSONArray("timeseries").getJSONObject(0).getJSONArray("data").getJSONObject(getTimegrain(thisMetricConfig.getTimeSpan()) - 1);
                    if (jsonValue.keySet().contains(thisMetricConfig.getAggregationType().toLowerCase())) {
                        Double value = (Double) jsonValue.get(thisMetricConfig.getAggregationType().toLowerCase());
                        Metric metric = new Metric(metricName, Double.toString(value), metricPrefix + resourceName + METRIC_PATH_SEPARATOR + metricName);
                        metrics.add(metric);
                    } else
                        LOGGER.trace("Doesn't match the aggregation type : ", jsonValue);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while parsing the JSONArray", e);
        } finally {
            return metrics;
        }
    }

    private MetricConfig matchMetricConfig(String metricName, List<MetricConfig> metricConfigs) {
        for (MetricConfig metricConfig : metricConfigs) {
            if (metricConfig.getAttr().equals(metricName))
                return metricConfig;
        }
        return null;
    }

    private int getTimegrain(String grain) {
        Matcher m = p.matcher(grain);

        // if an occurrence if a pattern was found in a given string...
        if (m.find()) {
            return Integer.parseInt(m.group(0));
        }
        return 1;
    }
}
