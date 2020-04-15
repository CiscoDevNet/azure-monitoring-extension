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
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.AssertUtils;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger("AzureCustomNamespaceMonitor.class");
    private static final String DEFAULT_METRIC_PREFIX = String.format("%s%s%s%s", "Custom Metrics", METRIC_PATH_SEPARATOR, "Azure CustomNameSpace", METRIC_PATH_SEPARATOR);
    private MonitorContextConfiguration monitorContextConfiguration;

    public AzureCustomNamespaceMonitor() {
        super(Configuration.class);
        LOGGER.info(String.format("Using Azure Custom Namespace Monitor Version [%s]",
                this.getClass().getPackage().getImplementationTitle()));
    }

    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    public String getMonitorName() {
        return monitorName;
    }

    @Override
    protected List<Map<String, ?>> getAccounts() {
        return null;
    }

    @Override
    protected List<Metric> getStatsForUpload(TasksExecutionServiceProvider tasksExecutionServiceProvider, Configuration config) {
        List<Metric> collectedMetrics = Arrays.asList();
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
            LOGGER.error("Error in Azure CustomNameSpace monitoring extension, while processing the account", e);
        }
        return collectedMetrics;
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

    @Override
    protected List<Map<String, ?>> getServers() {
        Map<String, String> serversMap = new HashMap<String, String>();
        List<Map<String, ?>> serversList = new ArrayList<Map<String, ?>>();
        serversList.add(serversMap);
        return serversList;
    }

    public static void main(String[] args) throws TaskExecutionException {
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        AzureCustomNamespaceMonitor monitor = new AzureCustomNamespaceMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();

        taskArgs.put(CONFIG_FILE, "src/main/resources/config.yml");
        taskArgs.put(METRIC_FILE, "src/main/resources/metrics.xml");

        monitor.execute(taskArgs, null);

    }
}

