/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.config;

import com.appdynamics.extensions.controller.ControllerInfo;

import java.util.List;

/**
 * @author PRASHANT MEHTA
 */
public class Configuration {

    private List<Account> accounts;

    private ProxyConfig proxyConfig;

    private ConcurrencyConfig concurrencyConfig;

    private MetricsTimeRange metricsTimeRange;

    private int numberOfThreads;

    private Connection connection;

    private boolean filterStats;

    private ControllerInfo controllerInfo;

    private String metricPrefix;

    private String encryptionKey;

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public ConcurrencyConfig getConcurrencyConfig() {
        return concurrencyConfig;
    }

    public void setConcurrencyConfig(ConcurrencyConfig concurrencyConfig) {
        this.concurrencyConfig = concurrencyConfig;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public MetricsTimeRange getMetricsTimeRange() {
        return metricsTimeRange;
    }

    public void setMetricsTimeRange(MetricsTimeRange metricsTimeRange) {
        this.metricsTimeRange = metricsTimeRange;
    }

    public boolean isFilterStats() {
        return filterStats;
    }

    public void setFilterStats(boolean filterStats) {
        this.filterStats = filterStats;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public ControllerInfo getControllerInfo() {
        return controllerInfo;
    }

    public void setControllerInfo(ControllerInfo controllerInfo) {
        this.controllerInfo = controllerInfo;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
}
