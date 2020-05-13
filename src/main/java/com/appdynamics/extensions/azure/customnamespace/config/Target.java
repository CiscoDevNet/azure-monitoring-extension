package com.appdynamics.extensions.azure.customnamespace.config;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/


import com.google.common.collect.Lists;

import java.util.List;

/**
 * Target class for the account targets specified in the config.yml
 */
public class Target {
    public String DisplayName;
    public String resource;
    public List<MetricConfig> metrics;
    public List<String> serviceInstances;
    public List<String> resourceGroups;

    public List<String> getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(List<String> resourceGroups) {
        this.resourceGroups = resourceGroups;
    }


    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setServiceInstances(List<String> serviceInstances){
        this.serviceInstances = serviceInstances;
    }

    public List<String> getServiceInstances(){
        return this.serviceInstances;
    }
    public List<MetricConfig> getMetrics() {
        if(metrics == null)
            return Lists.newArrayList();
        return metrics;
    }

    public void setMetrics(List<MetricConfig> metrics) {
        this.metrics = metrics;
    }

    public String getDisplayName() {
        return DisplayName;
    }

    public void setDisplayName(String displayName) {
        DisplayName = displayName;
    }
}