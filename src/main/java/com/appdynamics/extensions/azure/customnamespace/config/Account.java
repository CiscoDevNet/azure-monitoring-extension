/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Prashant Mehta
 */
public class Account {

    private String displayName;

    private Set<String> resourceGroups;

    private String service;

    private Set<String> serviceInstances;
    
    private String interval;

    private Set<String> regions;

    private Map<String, List<String>> dimensions;

    private Credentials credentials;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<String> getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(Set<String> resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Set<String> getRegions() {
        return regions;
    }

    public void setRegions(Set<String> regions) {
        this.regions = regions;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public Map<String, List<String>> getDimensions() {
        return dimensions;
    }

    public void setDimensions(Map<String, List<String>> dimensions) {
        this.dimensions = dimensions;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public Set<String> getServiceInstances() {
        return serviceInstances;
    }

    public void setServiceInstances(Set<String> serviceInstances) {
        this.serviceInstances = serviceInstances;
    }
}
