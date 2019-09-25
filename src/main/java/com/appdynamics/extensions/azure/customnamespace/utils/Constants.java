/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.utils;/*
 * Copyright 2019. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

public class Constants {
    public static final String METRIC_PATH_SEPARATOR;
    public static final String monitorName;
    public static final String CONFIG_FILE;
    public static final String METRIC_FILE;
    public static final String DISPLAY_NAME;
    public static final int MONITORED_RESOURCE_LIMIT;
    public static final String SERVICE;
    public static final String CONTAINER_INSTANCES;
    public static final String SQL_SERVER;
    public static final String STORAGE_ACCOUNT;
    public static final String VIRTUAL_MACHINE;
    public static final String INTERVAL;
    public static final String REGIONS;
    public static final String RESOURCE_GROUPS;
    static {
        METRIC_PATH_SEPARATOR = "|";
        monitorName = "CustomNameSpaceMonitor";
        CONFIG_FILE = "config-file";
        METRIC_FILE = "metric-file";
        DISPLAY_NAME = "displayName";
        MONITORED_RESOURCE_LIMIT = 100;
        SERVICE = "service";
        CONTAINER_INSTANCES = "Container instances";
        SQL_SERVER = "SQL server";
        STORAGE_ACCOUNT = "Storage account";
        VIRTUAL_MACHINE = "Virtual machine";
        INTERVAL = "interval";
        REGIONS = "regions";
        RESOURCE_GROUPS = "resourceGroups";
    }
}
