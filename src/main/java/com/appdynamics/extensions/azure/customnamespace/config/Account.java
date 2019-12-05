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

/**
 * @author Prashant Mehta
 */
public class Account {

    private String displayName;

    private List<Service> services;

    private Map<String, List<String>> dimensions;

    private Credentials credentials;

    private List<Target> targets;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public List<Target> getTargets() {
        return targets;
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }
}
