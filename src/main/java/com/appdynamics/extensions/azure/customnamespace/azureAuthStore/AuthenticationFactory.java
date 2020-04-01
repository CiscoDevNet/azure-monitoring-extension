package com.appdynamics.extensions.azure.customnamespace.azureAuthStore;

import com.appdynamics.extensions.azure.customnamespace.config.Credentials;
import com.appdynamics.extensions.azure.customnamespace.utils.Constants;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.management.Azure;

import javax.naming.ServiceUnavailableException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    private static final String AUTHORITY = Constants.AUTHORITY;

    public static Azure getAzure(Credentials accountCreds) throws IOException {

        client = accountCreds.getClient();
        tenant = accountCreds.getTenant();
        secret = accountCreds.getSecret();
        subscriptionId = accountCreds.getSubscriptionId();
        certAuthFilePath = accountCreds.getCertAuthFilePath();

        if (secret != null || !secret.equals("")) {
            ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(client, tenant, secret, AzureEnvironment.AZURE);
            return Azure.authenticate(credentials).withSubscription(subscriptionId);
        } else if (certAuthFilePath != null || !certAuthFilePath.equals("")) {
            return Azure.authenticate(new File(certAuthFilePath)).withSubscription(subscriptionId);
        } else
            return Azure.authenticate(AzureCliCredentials.create()).withDefaultSubscription();

    }

    public static AuthenticationResult getAccessTokenFromUserCredentials() throws Exception {
        AuthenticationResult result = null;
        //TODO: check the executor
        ExecutorService service = Executors.newFixedThreadPool(1);
        try {
            ClientCredential credential = new ClientCredential(client, secret);
            AuthenticationContext context = new AuthenticationContext(AUTHORITY + tenant, false, service);
            Future<AuthenticationResult> future = context.acquireToken(Constants.AZURE_MANAGEMENT, credential, null);
            result = future.get();
        } finally {
            service.shutdown();
        }
        if (result == null) {
            throw new ServiceUnavailableException("authentication result was null");
        }
        return result;
    }

}
