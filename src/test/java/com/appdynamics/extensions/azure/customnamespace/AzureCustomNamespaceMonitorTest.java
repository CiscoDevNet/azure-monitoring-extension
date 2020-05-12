package com.appdynamics.extensions.azure.customnamespace;

import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.Map;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureCustomNamespaceMonitorTest {

    @Test
    public void test() throws TaskExecutionException {
        AzureCustomNamespaceMonitor monitor = new AzureCustomNamespaceMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put("config-file", "src/test/resources/config.yml");
        taskArgs.put("metric-file", "src/test/resources/metrics.xml");
        monitor.execute(taskArgs, null);
    }
}