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
import com.appdynamics.extensions.azure.customnamespace.utils.CommonUtilities;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TargetUtils {

    private static final Pattern p = Pattern.compile("[^\\d]*[\\d]+[^\\d]+([\\d]+)");

    public  static Map<String, List<Map<String, String>>> httpClientConfigTransformer(Configuration configuration, Target target){
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<Map<String, String>>> config = mapper.convertValue(configuration, Map.class);
        Map<String, String> server = mapper.convertValue(target, Map.class);
        List<Map<String, String>> serversList = Lists.newArrayList();
        serversList.add(server);
        config.put("servers", serversList);
        config.remove("accounts");
        return config;
    }

    public static List<String> filterConfiguredResourceNames(List<String> resourceNames, List<String> targetResourceNames) {
        List<String> matchedResourceNames = Lists.newArrayList();
        for (String resourceName : resourceNames)
            if (CommonUtilities.checkStringPatternMatch(resourceName, targetResourceNames))
                matchedResourceNames.add(resourceName);

        return matchedResourceNames;
    }

    public static Boolean matchedAndModifiedAttr(MetricConfig metricStat, List<MetricConfig> actualMetricConfigs) {
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

    public static MetricConfig matchMetricConfig(String metricName, List<MetricConfig> metricConfigs) {
        for (MetricConfig metricConfig : metricConfigs) {
            if (metricConfig.getAttr().equals(metricName))
                return metricConfig;
        }
        return null;
    }

    public static int getTimegrain(String grain) {
        Matcher m = p.matcher(grain);
        if (m.find()) {
            return Integer.parseInt(m.group(0));
        }
        return 1;
    }
    public static String apiMetricNamesBuilder(List<MetricConfig> metricConfigs) {
        StringBuilder metricsQuery = new StringBuilder();
        metricsQuery.append("&metricnames=");
        StringJoiner sj = new StringJoiner(",");
        for (MetricConfig metricConfig : metricConfigs) {
            sj.add(metricConfig.getAttr());
        }
        metricsQuery.append(sj.toString()).append("&");
        return metricsQuery.toString();
    }


    public static void scanJsonResponseforMetricConfigs(JSONObject jsonObject, List<MetricConfig> actualMetricConfigs) {
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

}
