package com.appdynamics.extensions.azure.customnamespace.config;


import static org.apache.commons.lang3.StringUtils.capitalize;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class convert {

    public static void convertTo(String[] arr) {

        for (String str : arr) {
            String myservice = capitalize(str);
            System.out.println("  else if (service instanceof " + myservice + " && checkServiceRegionPatternMatch((( " + myservice + " ) service).name(), serviceInstances, (( " + myservice + " ) service).region().label(), regions)){\n" +
                    "            resourceId = ((( " + myservice + " ) service).id());}");
        }
    }

    public static void main(String[] args) {
        String[] arr = {"applicationGateway", "applicationSecurityGroup", "autoscaleSetting", "availabilitySet", "batchAIWorkspace", "batchAccount", "cdnProfile", "containerServices", "ddosProtectionPlan", "deployment", "dnsZone", "eventHubNamespace", "expressRouteCircuit", "expressRouteCrossConnection", "gallery", "genericResource", "identities", "kubernetesCluster", "loadBalancer", "localNetworkGateway", "managementLock", "networkInterface", "networkSecurityGroup", "networkWatcher", "network", "policyAssignment", "publicIPAddresses", "redisCache", "containerRegistry", "routeFilter", "routeTable", "searchService", "serviceBusNamespace", "snapshot", "trafficManagerProfile", "vault", "virtualMachineCustomImage", "virtualMachineScaleSet", "virtualNetworkGateway"};
        convertTo(arr);
    }
}
