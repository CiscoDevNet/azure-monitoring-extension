package com.appdynamics.extensions.azure.customnamespace;

import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons.AzureMonitor;
import com.appdynamics.extensions.azure.customnamespace.config.Account;
import com.appdynamics.extensions.azure.customnamespace.config.Configuration;
import com.appdynamics.extensions.azure.customnamespace.config.Stat;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.CONFIG_FILE;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.METRIC_FILE;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.METRIC_PATH_SEPARATOR;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureCustomNamespaceMonitor extends AzureMonitor<Configuration> {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureCustomNamespaceMonitor.class);
    private static final String DEFAULT_METRIC_PREFIX = String.format("%s%s%s%s", "Custom Metrics", METRIC_PATH_SEPARATOR, "Azure", METRIC_PATH_SEPARATOR);
    private MonitorContextConfiguration monitorContextConfiguration;

    public AzureCustomNamespaceMonitor() {
        super(Configuration.class);
        LOGGER.info(String.format("Using Azure Monitor Version [%s]",
                this.getClass().getPackage().getImplementationTitle()));
    }

    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void getStatsForUpload(TasksExecutionServiceProvider tasksExecutionServiceProvider, Configuration config) {
        try {
            List<Account> accounts = config.getAccounts();
            String metricPrefix = config.getMetricPrefix();
            for (Account account : accounts) {
                AssertUtils.assertNotNull(monitorContextConfiguration.getMetricsXml(), "Metrics xml not available");
                AssertUtils.assertNotNull(account, "the account arguments are empty");
                AzureCustomNamespaceMonitorTask task = new AzureCustomNamespaceMonitorTask(monitorContextConfiguration, config, tasksExecutionServiceProvider.getMetricWriteHelper(), account, metricPrefix);
                tasksExecutionServiceProvider.submit("accounts", task);
            }
        } catch (Exception e) {
            LOGGER.error("Error in Azure monitoring extension, while processing the account", e);
        }
    }

    protected org.slf4j.Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        monitorContextConfiguration = getContextConfiguration();
        LOGGER.info("initializing metric.xml file");
        monitorContextConfiguration.setMetricXml(args.get("metric-file"), Stat.Stats.class);
    }

    public static void main(String[] args) throws TaskExecutionException {

        AzureCustomNamespaceMonitor monitor = new AzureCustomNamespaceMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();

        taskArgs.put(CONFIG_FILE, "src/main/resources/config.yml");
        taskArgs.put(METRIC_FILE, "src/main/resources/metrics.xml");

        monitor.execute(taskArgs, null);

    }
}

