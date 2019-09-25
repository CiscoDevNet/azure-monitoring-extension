package com.appdynamics.extensions.azure.customnamespace.metricStatistics;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class ResourceGroupMetricStatistics {
    private MetricStatistics metrics;
    private Double value;
    private String unit;
    private String metricPrefix;

    public ResourceGroupMetricStatistics() {
    }

    public MetricStatistics getMetric() {
        return this.metrics;
    }

    public void setMetric(AzureMetric metric) {
        this.metrics = metrics;
    }

    public Double getValue() {
        return this.value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return this.unit;
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
