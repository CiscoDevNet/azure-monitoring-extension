package com.appdynamics.extensions.azure.customnamespace.metricStatistics;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class MetricStatistics {
    private AzureMetric metric;
    private String unit;
    private String metricPrefix;

    public MetricStatistics() {
    }

    public AzureMetric getMetric() {
        return this.metric;
    }

    public void setMetric(AzureMetric metric) {
        this.metric = metric;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getMetricPrefix() {
        return this.metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
