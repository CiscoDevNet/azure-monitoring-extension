/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.config;

import java.util.List;

/**
 * @author PRASHANT MEHTA
 */
public class Configuration {

    private List<Account> accounts;

    private String metricPrefix;

    private String service;

    private CredentialsDecryptionConfig credentialsDecryptionConfig;

    private ProxyConfig proxyConfig;

    private ConcurrencyConfig concurrencyConfig;

    private MetricsTimeRange metricsTimeRange;

    private int numberOfThreads;

    private TaskSchedule taskSchedule;

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public CredentialsDecryptionConfig getCredentialsDecryptionConfig() {
        return credentialsDecryptionConfig;
    }

    public void setCredentialsDecryptionConfig(
            CredentialsDecryptionConfig credentialsDecryptionConfig) {
        this.credentialsDecryptionConfig = credentialsDecryptionConfig;
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

    public TaskSchedule getTaskSchedule() {
        return taskSchedule;
    }

    public void setTaskSchedule(TaskSchedule taskSchedule) {
        this.taskSchedule = taskSchedule;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public MetricsTimeRange getMetricsTimeRange() {
        return metricsTimeRange;
    }

    public void setMetricsTimeRange(MetricsTimeRange metricsTimeRange) {
        this.metricsTimeRange = metricsTimeRange;
    }

}
