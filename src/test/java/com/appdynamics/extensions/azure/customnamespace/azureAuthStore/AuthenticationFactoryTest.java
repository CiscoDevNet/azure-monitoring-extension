/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.azureAuthStore;

import com.appdynamics.extensions.azure.customnamespace.config.Credentials;
import com.microsoft.azure.management.Azure;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class AuthenticationFactoryTest {

    private Credentials creds = new Credentials();

    @Before
    public void setUp(){
        creds.setCertAuthFilePath("path to file");
        creds.setClient("myclientID");
        creds.setSecret("mySecretId");
        creds.setSubscriptionId("mySubbscriptionId");
        creds.setTenant("myTenantId");

    }


    @Test
    public void getAzureFromConfigCredsTTest() throws IOException {
        Azure azure = AuthenticationFactory.getAzure(creds, "");
        Assert.assertNotNull(azure);
        Assert.assertEquals("mySubbscriptionId", azure.subscriptionId());

    }

    @Test
    public void getAzureFromConfigEEncryptedCredsTTest() throws Exception {
        creds.setTenant("j+0oxTzUtw2xAdaq4UUq/Q==");
        creds.setSecret("+lEtFnV04JQaWL05TfWJ/g==");
        Azure azure = AuthenticationFactory.getAzure(creds,  "abcd");
        Assert.assertNotNull(azure);
        Assert.assertEquals("mySubbscriptionId", azure.subscriptionId());
    }

}