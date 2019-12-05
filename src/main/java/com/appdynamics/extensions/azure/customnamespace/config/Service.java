package com.appdynamics.extensions.azure.customnamespace.config;

import java.util.List;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class Service {
    private String serviceName;
    private List<String> resourceGroups;
    private List<String> regions;
    private List<String> serviceInstances;


    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<String> getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(List<String> resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }

    public List<String> getServiceInstances() {
        return serviceInstances;
    }

    public void setServiceInstances(List<String> serviceInstances) {
        this.serviceInstances = serviceInstances;
    }
}
