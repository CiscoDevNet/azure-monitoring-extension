package com.appdynamics.extensions.azure.customnamespace.AzureNamespaceGroupFactory;

import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.CONTAINER_INSTANCES;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.SQL_SERVER;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.STORAGE_ACCOUNT;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.VIRTUAL_MACHINE;
import com.microsoft.azure.management.Azure;

import java.util.List;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class NameSpaceGroupFactory {

//    Had to use generics as the return types vary with the services and there is no common base class either.
    public static <T> List<T> getNamespaceGroup(Azure azure, String namespace, String resourceGroup){
        if(namespace.equals(CONTAINER_INSTANCES))
            return (List<T>) azure.containerGroups().listByResourceGroup(resourceGroup);
        else if(namespace.equals(SQL_SERVER))
            return (List<T>) azure.sqlServers().listByResourceGroup(resourceGroup);
        else if(namespace.equals(STORAGE_ACCOUNT))
            return (List<T>) azure.storageAccounts().listByResourceGroup(resourceGroup);
        else if(namespace.equals(VIRTUAL_MACHINE))
            return (List<T>) azure.virtualMachines().listByResourceGroup(resourceGroup);
        return null;
    }


}
