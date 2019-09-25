package com.appdynamics.extensions.azure.customnamespace.azureAuthStore;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.management.Azure;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/


//reference : https://github.com/Azure/azure-libraries-for-java/blob/master/AUTH.md
public class AuthenticationFactory {
    public static String client;
    public static String tenant;
    public static String secret;
    public static String subscriptionId;
    public static String certAuthFilePath;

    public static Azure getAzure(Map<String, ?> accountCreds) throws IOException {
        client = (String) accountCreds.get("client");
        tenant = (String) accountCreds.get("tenant");
        secret = (String) accountCreds.get("secret");
        subscriptionId = (String) accountCreds.get("subscriptionId");
        certAuthFilePath = (String) accountCreds.get("certAuthFilePath");

        if (secret != null || !secret.equals("")) {
            ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(client, tenant, secret, AzureEnvironment.AZURE);
            return Azure.authenticate(credentials).withSubscription(subscriptionId);
        } else if (certAuthFilePath != null || !certAuthFilePath.equals("")) {
            return Azure.authenticate(new File(certAuthFilePath)).withSubscription(subscriptionId);
        } else
            return Azure.authenticate(AzureCliCredentials.create()).withDefaultSubscription();

    }

}
