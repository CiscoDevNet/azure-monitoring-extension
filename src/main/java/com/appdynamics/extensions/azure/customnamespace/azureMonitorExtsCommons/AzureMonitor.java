package com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public abstract class AzureMonitor<T> extends ABaseMonitor {

    private Class<T> clazz;
    private T config;

    public AzureMonitor(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    protected void onConfigReload(File file) {
        Yaml yaml = new Yaml();
        try {
            config = yaml.loadAs(new FileInputStream(file), clazz);
        } catch (FileNotFoundException e) {
            getLogger().error("Error wile reading the config file", e);
        }
    }

    protected String getDefaultMetricPrefix() {
        return null;
    }

    public String getMonitorName() {
        return null;
    }


    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {
        getLogger().info("Starting Azure Cloud Monitoring task");

        try {
            getStatsForUpload(serviceProvider, config);

            serviceProvider.getMetricWriteHelper().onComplete();

        } catch (Exception ex) {
            getLogger().error("Unfortunately an issue has occurred: ", ex);
        }
    }

    protected abstract void getStatsForUpload(TasksExecutionServiceProvider serviceProvider, T config);

    protected abstract Logger getLogger();

    protected List<Map<String, ?>> getServers() {
        List<Map<String, ?>> serversList = Lists.newArrayList();
        return serversList;
    }

}
