package com.appdynamics.extensions.azure.customnamespace;

import static com.appdynamics.extensions.azure.customnamespace.IntegrationTestUtils.initializeMetricAPIService;
import com.appdynamics.extensions.controller.apiservices.MetricAPIService;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * @author Prashant Mehta
 */
public class MetricCheckIT {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(MetricCheckIT.class);

    private static final String USER_AGENT = "Mozilla/5.0";

    private CloseableHttpClient httpClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MetricAPIService metricAPIService;

    @Before
    public void setup() {
        metricAPIService = initializeMetricAPIService();
    }

    @After
    public void tearDown() {
        //todo: shutdown client
    }

    @Test
    public void testAPICallsMetric() {
        JsonNode jsonNode = null;
        if (metricAPIService != null) {
            logger.debug("metrricApiService", metricAPIService.toString());
            jsonNode = metricAPIService.getMetricData("","Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CAzure%7CMetrics%20Uploaded&time-range-type=BEFORE_NOW&duration-in-mins=15&output=JSON");
        }
        logger.debug("Ouput from getMetricData", jsonNode.toString());
        Assert.assertNotNull("Cannot connect to controller API", jsonNode);
        JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricId");
        Assert.assertTrue("Azure Metrics Uploaded", valueNode.get(0).asInt() > 0);

    }
}