package com.appdynamics.extensions.azure.customnamespace.config;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class Credentials {
    private String client;
    private String tenant;
    private String secret;
    private String subscriptionId;
    private String certAuthFilePath;

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getCertAuthFilePath() {
        return certAuthFilePath;
    }

    public void setCertAuthFilePath(String certAuthFilePath) {
        this.certAuthFilePath = certAuthFilePath;
    }
}
