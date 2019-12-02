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
    public static final String MonitorName;
    public static final String METRIC_PATH_SEPARATOR;
    public static final String CONFIG_FILE;
    public static final String METRIC_FILE;
    public static final String DISPLAY_NAME;
    public static final String AUTHORITY;
    public static final String AZURE_MANAGEMENT;
    public static final String ACTIVE_DIRECTORY_AUTHORITY_URL;
    public static final int MONITORED_RESOURCE_LIMIT;
    public static final String SERVICE;
    public static final String INTERVAL;
    public static final String REGIONS;
    public static final String RESOURCE_GROUPS;
    public static final String ACTION_GROUP;
    public static final String APPLICATION_GATEWAY;
    public static final String APPLICATION_SECURITY_GROUP;
    public static final String AUTOSCALE_SETTING;
    public static final String AVAILABILITY_SET;
    public static final String BATCH_AI_WORKSPACE;
    public static final String BATCH_ACCOUNT;
    public static final String CDN_PROFILE;
    public static final String CONTAINER_GROUP;
    public static final String CONTAINER_SERVICE;
    public static final String COSMOS_DB_ACCOUNT;
    public static final String DDOS_PROTECTION_PLAN;
    public static final String DEPLOYMENT;
    public static final String DISK;
    public static final String DNS_ZONE;
    public static final String EVENT_HUB_NAMESPACE;
    public static final String EXPRESS_ROUTE_CIRCUIT;
    public static final String EXPRESS_ROUTE_CROSS_CONNECTION;
    public static final String GALLERY;
    public static final String GENERIC_RESOURCE;
    public static final String IDENTITY;
    public static final String KUBERNETES_CLUSTER;
    public static final String LOAD_BALANCER;
    public static final String LOCAL_NETWORK_GATEWAY;
    public static final String MANAGEMENT_LOCK;
    public static final String NETWORK_INTERFACE;
    public static final String NETWORK_SECURITY_GROUP;
    public static final String NETWORK_WATCHER;
    public static final String NETWORK;
    public static final String POLICY_ASSIGNMENT;
    public static final String PUBLIC_IP_ADDRESS;
    public static final String REDIS_CACHE;
    public static final String REGISTRY;
    public static final String ROUTE_FILTER;
    public static final String ROUTE_TABLE;
    public static final String SEARCH_SERVICE;
    public static final String SERVICE_BUS_NAMESPACE;
    public static final String SNAPSHOT;
    public static final String SQL_SERVER;
    public static final String STORAGE_ACCOUNT;
    public static final String TRAFFIC_MANAGER_PROFILE;
    public static final String VAULT;
    public static final String VIRTUAL_MACHINE_CUSTOM_IMAGE;
    public static final String VIRTUAL_MACHINE_SCALE_SET;
    public static final String VIRTUAL_MACHINE;
    public static final String VIRTUAL_NETWORK_GATEWAY;
    public static final String WEB_APP;
    static {
        METRIC_PATH_SEPARATOR = "|";
        MonitorName = "CustomNameSpaceMonitor";
        CONFIG_FILE = "config-file";
        METRIC_FILE = "metric-file";
        DISPLAY_NAME = "displayName";
        AUTHORITY  = "https://login.windows.net/";
        AZURE_MANAGEMENT = "https://management.azure.com/";
        ACTIVE_DIRECTORY_AUTHORITY_URL = "https://login.microsoftonline.com/";
        MONITORED_RESOURCE_LIMIT = 100;
        SERVICE = "service";
        INTERVAL = "interval";
        REGIONS = "regions";
        CONTAINER_GROUP = "Container instances";
        SQL_SERVER = "SQL server";
        STORAGE_ACCOUNT = "Storage account";
        VIRTUAL_MACHINE = "Virtual machine";
        RESOURCE_GROUPS = "resourceGroups";
        ACTION_GROUP = "ActionGroup";
        APPLICATION_GATEWAY = "ApplicationGateway";
        APPLICATION_SECURITY_GROUP = "ApplicationSecurityGroup";
        AUTOSCALE_SETTING = "AutoscaleSetting";
        AVAILABILITY_SET = "AvailabilitySet";
        BATCH_AI_WORKSPACE = "BatchAIWorkspace";
        BATCH_ACCOUNT = "BatchAccount";
        CDN_PROFILE = "CdnProfile";
        CONTAINER_SERVICE = "ContainerService";
        COSMOS_DB_ACCOUNT = "Cosmos DB account";
        DDOS_PROTECTION_PLAN = "DdosProtectionPlan";
        DEPLOYMENT = "Deployment";
        DISK = "Disk";
        DNS_ZONE = "DnsZone";
        EVENT_HUB_NAMESPACE = "EventHubNamespace";
        EXPRESS_ROUTE_CIRCUIT = "ExpressRouteCircuit";
        EXPRESS_ROUTE_CROSS_CONNECTION = "ExpressRouteCrossConnection";
        GALLERY = "Gallery";
        GENERIC_RESOURCE = "GenericResource";
        IDENTITY = "Identity";
        KUBERNETES_CLUSTER = "KubernetesCluster";
        LOAD_BALANCER = "LoadBalancer";
        LOCAL_NETWORK_GATEWAY = "LocalNetworkGateway";
        MANAGEMENT_LOCK = "ManagementLock";
        NETWORK_INTERFACE = "NetworkInterface";
        NETWORK_SECURITY_GROUP = "NetworkSecurityGroup";
        NETWORK_WATCHER = "NetworkWatcher";
        NETWORK = "Network";
        POLICY_ASSIGNMENT = "PolicyAssignment";
        PUBLIC_IP_ADDRESS = "PublicIPAddress";
        REDIS_CACHE = "RedisCache";
        REGISTRY = "Registry";
        ROUTE_FILTER = "RouteFilter";
        ROUTE_TABLE = "RouteTable";
        SEARCH_SERVICE = "SearchService";
        SERVICE_BUS_NAMESPACE = "ServiceBusNamespace";
        SNAPSHOT = "Snapshot";
        TRAFFIC_MANAGER_PROFILE = "TrafficManagerProfile";
        VAULT = "Vault";
        VIRTUAL_MACHINE_CUSTOM_IMAGE = "VirtualMachineCustomImage";
        VIRTUAL_MACHINE_SCALE_SET = "VirtualMachineScaleSet";
        VIRTUAL_NETWORK_GATEWAY = "VirtualNetworkGateway";
        WEB_APP = "WebApp";

    }
}
