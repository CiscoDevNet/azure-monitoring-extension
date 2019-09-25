package com.appdynamics.extensions.azure.customnamespace.metricStatistics;

import com.appdynamics.extensions.azure.customnamespace.config.IncludeMetric;
import com.appdynamics.extensions.metrics.Metric;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureMetric {
    private IncludeMetric includeMetric;
    private Metric metric;

    public AzureMetric() {
    }

    public IncludeMetric getIncludeMetric() {
        return this.includeMetric;
    }

    public void setIncludeMetric(IncludeMetric includeMetric) {
        this.includeMetric = includeMetric;
    }

    public Metric getMetric() {
        return this.metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }
}
