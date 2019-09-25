/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.config;


public class IncludeMetric {

    private String name;

    private String alias;

    private String statType;

    private String aggregationType;

    private String timeRollUpType;

    private String clusterRollUpType;

    private double multiplier;

    private boolean delta;

    private MetricsTimeRange metricsTimeRange;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatType() {
        return statType;
    }

    public void setStatType(String statType) {
        this.statType = statType;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public String getTimeRollUpType() {
        return timeRollUpType;
    }

    public void setTimeRollUpType(String timeRollUpType) {
        this.timeRollUpType = timeRollUpType;
    }

    public String getClusterRollUpType() {
        return clusterRollUpType;
    }

    public void setClusterRollUpType(String clusterRollUpType) {
        this.clusterRollUpType = clusterRollUpType;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public boolean isDelta() {
        return delta;
    }

    public void setDelta(boolean delta) {
        this.delta = delta;
    }

    public MetricsTimeRange getMetricsTimeRange() {
        return metricsTimeRange;
    }

    public void setMetricsTimeRange(MetricsTimeRange metricsTimeRange) {
        this.metricsTimeRange = metricsTimeRange;
    }
}
