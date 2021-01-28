/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.azureTarget;

import com.appdynamics.extensions.azure.customnamespace.config.Account;
import com.appdynamics.extensions.azure.customnamespace.config.MetricConfig;
import com.appdynamics.extensions.azure.customnamespace.config.Target;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.AUTHORIZATION;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.BEARER;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.microsoft.aad.adal4j.AuthenticationResult;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;

public class TimegrainTargetCollectorTask implements Callable {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(TimegrainTargetCollectorTask.class);

    private Target target;
    private Account account;
    private AuthenticationResult authTokenResult;
    private HttpClient client;
    private String url;
    private Map.Entry<String, List<MetricConfig>> grainEntry;
    private String resourceName;
    private String metricPrefix;
    private LongAdder requestCounter;

    private String METRICS_API_VERSION = "2018-01-01";
    private String AGGREGATION_TYPES = "Total,Average,Maximum,Minimum,Count";
    private String loggingPrefix;

    public TimegrainTargetCollectorTask(Builder builder) {
        this.target = builder.target;
        this.account = builder.account;
        this.authTokenResult = builder.authTokenResult;
        this.client = builder.client;
        this.url = builder.url;
        this.grainEntry = builder.grainEntry;
        this.resourceName = builder.resourceName;
        this.metricPrefix = builder.metricPrefix;
        this.requestCounter = builder.requestCounter;

        this.loggingPrefix = "[ACCOUNT=" + this.account.getDisplayName() + ", TARGET=" + this.target.getDisplayName() + ", RESOURCE=" + this.resourceName + "]";
    }

    @Override
    public Object call() {
        List<Metric> metrics = Lists.newArrayList();
        try {
            metricsCollector(metrics);
        } catch (Exception e) {
            LOGGER.error("{} - Exception while gathering granular metric collections", loggingPrefix, e);
        }
        return metrics;
    }

    private void metricsCollector(List<Metric> metrics) throws IOException {
        String subUrl = TargetUtils.apiMetricNamesBuilder(grainEntry.getValue());
        String requestURL = (url + METRICS_API_VERSION + subUrl + "&aggregation=" + AGGREGATION_TYPES + "&timespan=" + grainEntry.getKey()).replaceAll("\\s+", "%20");
        LOGGER.debug("{} - metrics values API request: ||{}||", loggingPrefix, requestURL);

        HttpGet request = new HttpGet(requestURL);
        request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());
        HttpResponse response = client.execute(request);
        LOGGER.debug("{} - metrics values API response status code: ||{}||", loggingPrefix, response.getStatusLine().getStatusCode());
        requestCounter.increment();

        if (response.getStatusLine().toString().contains("200 OK")) {
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            LOGGER.trace("{} - metrics values API response: ||{}||", loggingPrefix, responseBody);

            JSONObject jsonObject = new JSONObject(responseBody);
            TargetJsonResponseParser jsonResponseParser = new TargetJsonResponseParser(metricPrefix, account, target);
            
            metrics.addAll(jsonResponseParser.parseJsonObject(jsonObject, resourceName, grainEntry.getValue()));
        }
        else {
            LOGGER.warn("{} - metrics values API request failed.  Please review target configuration and resposne for details. Response: ||{}||", loggingPrefix, response);
        }
    }

    public static class Builder {
        private Target target;
        private Account account;
        private AuthenticationResult authTokenResult;
        private HttpClient client;
        private String url;
        private Map.Entry<String, List<MetricConfig>> grainEntry;
        private String resourceName;
        private String metricPrefix;
        private LongAdder requestCounter;

        public Builder withTarget(Target target) {
            this.target = target;
            return this;
        }

        public Builder withAccount(Account account) {
            this.account = account;
            return this;
        }

        public Builder withAuthenticationResult(AuthenticationResult authTokenResult) {
            this.authTokenResult = authTokenResult;
            return this;
        }

        public Builder withClient(HttpClient client) {
            this.client = client;
            return this;
        }

        public Builder withUrl(String url ) {
            this.url = url ;
            return this;
        }

        public Builder withGrainEntry(Map.Entry<String, List<MetricConfig>> grainEntry) {
            this.grainEntry = grainEntry;
            return this;
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder withMetricPrefix(String metricPrefix) {
            this.metricPrefix = metricPrefix;
            return this;
        }

        public Builder withRequestCounter(LongAdder requestCounter) {
            this.requestCounter = requestCounter;
            return this;
        }

        public TimegrainTargetCollectorTask build() {
            return new TimegrainTargetCollectorTask(this);
        }
    }

}
