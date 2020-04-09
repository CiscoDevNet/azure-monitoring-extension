package com.appdynamics.extensions.azure.customnamespace;

import static com.appdynamics.extensions.azure.customnamespace.IntegrationTestUtils.initializeMetricAPIService;
import com.appdynamics.extensions.controller.apiservices.MetricAPIService;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.JsonUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
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
            jsonNode = metricAPIService.getMetricData("", "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CAzure%7CAzure%20API%20Calls&time-range-type=BEFORE_NOW&duration-in-mins=15&output=JSON");
        }
        Assert.assertNotNull("Cannot connect to controller API", jsonNode);
        // Keeping this for deugging purpose
        JsonNode valueNode = null;
        try {
            System.out.println("JsonNode : " + jsonNode);
            valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricId");
            System.out.println("valueNode : " + valueNode);
            System.out.println("valueNode_assert_value : " + valueNode.get(0).getIntValue());
            Assert.assertTrue(jsonNode.toString(), false);
        } catch (AssertionError | Exception e) {
            // Dummy echo output
            System.out.println("Assert error: " + e);
            System.out.println("JsonNode: " + jsonNode + " valueNode : " + valueNode);
        }
        Assert.assertTrue("AWS API Calls", valueNode.get(0).getIntValue() > 0);

    }


}