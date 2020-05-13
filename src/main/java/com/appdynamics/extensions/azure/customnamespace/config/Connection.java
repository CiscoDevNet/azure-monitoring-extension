package com.appdynamics.extensions.azure.customnamespace.config;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/

public class Connection {
    private boolean sslCertCheckEnabled;
    private int socketTimeout;
    private int connectTimeout;

    public boolean isSslCertCheckEnabled() {
        return sslCertCheckEnabled;
    }

    public void setSslCertCheckEnabled(boolean sslCertCheckEnabled) {
        this.sslCertCheckEnabled = sslCertCheckEnabled;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
}
