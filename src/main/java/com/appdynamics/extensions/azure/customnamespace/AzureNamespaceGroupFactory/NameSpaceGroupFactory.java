package com.appdynamics.extensions.azure.customnamespace.AzureNamespaceGroupFactory;

import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.*;
import com.microsoft.azure.management.Azure;

import java.util.List;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class NameSpaceGroupFactory {

    //Supports all the implementation of SupportsListingByResourceGroup resources.
    public static <T> List<T> getNamespaceGroup(Azure azure, String namespace, String resourceGroup){
        if(namespace.equals(CONTAINER_GROUP))
            return (List<T>) azure.containerGroups().listByResourceGroup(resourceGroup);
        else if(namespace.equals(SQL_SERVER))
            return (List<T>) azure.sqlServers().listByResourceGroup(resourceGroup);
        else if(namespace.equals(STORAGE_ACCOUNT))
            return (List<T>) azure.storageAccounts().listByResourceGroup(resourceGroup);
        else if(namespace.equals(VIRTUAL_MACHINE))
            return (List<T>) azure.virtualMachines().listByResourceGroup(resourceGroup);
        else if(namespace.equals(ACTION_GROUP))
            return (List<T>) azure. actionGroups().listByResourceGroup(resourceGroup);
         else if(namespace.equals(APPLICATION_GATEWAY))
            return (List<T>) azure.applicationGateways().listByResourceGroup(resourceGroup);
        else if(namespace.equals(APPLICATION_SECURITY_GROUP))
            return (List<T>) azure.applicationSecurityGroups().listByResourceGroup(resourceGroup);
        else if(namespace.equals(AUTOSCALE_SETTING))
            return (List<T>) azure.autoscaleSettings().listByResourceGroup(resourceGroup);
        else if(namespace.equals(AVAILABILITY_SET))
            return (List<T>) azure.availabilitySets().listByResourceGroup(resourceGroup);
        else if(namespace.equals(BATCH_AI_WORKSPACE))
            return (List<T>) azure.batchAIWorkspaces().listByResourceGroup(resourceGroup);
        else if(namespace.equals(BATCH_ACCOUNT))
            return (List<T>) azure.batchAccounts().listByResourceGroup(resourceGroup);
        else if(namespace.equals(CDN_PROFILE))
            return (List<T>) azure.cdnProfiles().listByResourceGroup(resourceGroup);
        else if(namespace.equals(CONTAINER_SERVICE))
            return (List<T>) azure.containerServices().listByResourceGroup(resourceGroup);
        else if(namespace.equals(COSMOS_DB_ACCOUNT))
            return (List<T>) azure.cosmosDBAccounts().listByResourceGroup(resourceGroup);
        else if(namespace.equals(DDOS_PROTECTION_PLAN))
            return (List<T>) azure.ddosProtectionPlans().listByResourceGroup(resourceGroup);
        else if(namespace.equals(DEPLOYMENT))
            return (List<T>) azure.deployments().listByResourceGroup(resourceGroup);
        else if(namespace.equals(DISK))
            return (List<T>) azure.disks().listByResourceGroup(resourceGroup);
        else if(namespace.equals(DNS_ZONE))
            return (List<T>) azure.dnsZones().listByResourceGroup(resourceGroup);
        else if(namespace.equals(EVENT_HUB_NAMESPACE))
            return (List<T>) azure.eventHubNamespaces().listByResourceGroup(resourceGroup);
        else if(namespace.equals(EXPRESS_ROUTE_CIRCUIT))
            return (List<T>) azure.expressRouteCircuits().listByResourceGroup(resourceGroup);
        else if(namespace.equals(EXPRESS_ROUTE_CROSS_CONNECTION))
            return (List<T>) azure.expressRouteCrossConnections().listByResourceGroup(resourceGroup);
        else if(namespace.equals(GALLERY))
            return (List<T>) azure.galleries().listByResourceGroup(resourceGroup);
        else if(namespace.equals(GENERIC_RESOURCE))
            return (List<T>) azure.genericResources().listByResourceGroup(resourceGroup);
        else if(namespace.equals(IDENTITY))
            return (List<T>) azure.identities().listByResourceGroup(resourceGroup);
        else if(namespace.equals(KUBERNETES_CLUSTER))
            return (List<T>) azure.kubernetesClusters().listByResourceGroup(resourceGroup);
        else if(namespace.equals(LOAD_BALANCER))
            return (List<T>) azure.loadBalancers().listByResourceGroup(resourceGroup);
        else if(namespace.equals(LOCAL_NETWORK_GATEWAY))
            return (List<T>) azure.localNetworkGateways().listByResourceGroup(resourceGroup);
        else if(namespace.equals(MANAGEMENT_LOCK))
            return (List<T>) azure.managementLocks().listByResourceGroup(resourceGroup);
        else if(namespace.equals(NETWORK_INTERFACE))
            return (List<T>) azure.networkInterfaces().listByResourceGroup(resourceGroup);
        else if(namespace.equals(NETWORK_SECURITY_GROUP))
            return (List<T>) azure.networkSecurityGroups().listByResourceGroup(resourceGroup);
        else if(namespace.equals(NETWORK_WATCHER))
            return (List<T>) azure.networkWatchers().listByResourceGroup(resourceGroup);
        else if(namespace.equals(NETWORK))
            return (List<T>) azure.networks().listByResourceGroup(resourceGroup);
        else if(namespace.equals(POLICY_ASSIGNMENT))
            return (List<T>) azure.policyAssignments().listByResourceGroup(resourceGroup);
        else if(namespace.equals(PUBLIC_IP_ADDRESS))
            return (List<T>) azure.publicIPAddresses().listByResourceGroup(resourceGroup);
        else if(namespace.equals(REDIS_CACHE))
            return (List<T>) azure.redisCaches().listByResourceGroup(resourceGroup);
        else if(namespace.equals(REGISTRY))
            return (List<T>) azure.containerRegistries().listByResourceGroup(resourceGroup);
        else if(namespace.equals(ROUTE_FILTER))
            return (List<T>) azure.routeFilters().listByResourceGroup(resourceGroup);
        else if(namespace.equals(ROUTE_TABLE))
            return (List<T>) azure.routeTables().listByResourceGroup(resourceGroup);
        else if(namespace.equals(SEARCH_SERVICE))
            return (List<T>) azure.searchServices().listByResourceGroup(resourceGroup);
        else if(namespace.equals(SERVICE_BUS_NAMESPACE))
            return (List<T>) azure.serviceBusNamespaces().listByResourceGroup(resourceGroup);
        else if(namespace.equals(SNAPSHOT))
            return (List<T>) azure.snapshots().listByResourceGroup(resourceGroup);
        else if(namespace.equals(TRAFFIC_MANAGER_PROFILE))
            return (List<T>) azure.trafficManagerProfiles().listByResourceGroup(resourceGroup);
        else if(namespace.equals(VAULT))
            return (List<T>) azure.vaults().listByResourceGroup(resourceGroup);
        else if(namespace.equals(VIRTUAL_MACHINE_CUSTOM_IMAGE))
            return (List<T>) azure.virtualMachineCustomImages().listByResourceGroup(resourceGroup);
        else if(namespace.equals(VIRTUAL_MACHINE_SCALE_SET))
            return (List<T>) azure.virtualMachineScaleSets().listByResourceGroup(resourceGroup);
        else if(namespace.equals(VIRTUAL_NETWORK_GATEWAY))
            return (List<T>) azure.virtualNetworkGateways().listByResourceGroup(resourceGroup);
        else if(namespace.equals(APP_SERVICE))
            return (List<T>) azure.webApps().listByResourceGroup(resourceGroup);
        else if(namespace.equals(APPLICATION_GATEWAY))
            return (List<T>) azure.applicationGateways().listByResourceGroup(resourceGroup);
        else if(namespace.equals(APP_SERVICE_PLAN))
            return (List<T>) azure.appServices().appServicePlans().listByResourceGroup(resourceGroup);
        //Default
        return null;
    }

}