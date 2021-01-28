package com.appdynamics.extensions.azure.customnamespace.utils;

import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.AUTHORIZATION;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.BEARER;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.RESOURCE_VERSION_PATH;
import com.appdynamics.extensions.azure.customnamespace.config.Target;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.microsoft.aad.adal4j.AuthenticationResult;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureApiVersionStore {
    private static Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureApiVersionStore.class);
    private static Map<String, String> versionsMap = Maps.newConcurrentMap();
    private static Boolean updateVersionMap = false;

    static {
        readVersionMapFromFile();
    }

    public static void writeVersionMapToFile() {
        if (updateVersionMap.equals(false))
            return;
        try {
            Gson gson = new Gson();
            String versionMapStr = gson.toJson(versionsMap);
            File file = new File(RESOURCE_VERSION_PATH);
            if (file.createNewFile())
                LOGGER.debug("Loading the existing resource-version.json for updating");
            else
                LOGGER.debug("creating new File resource-version.json for writing");

            FileWriter writer = new FileWriter(file);
            writer.write(versionMapStr);
            writer.close();
            updateVersionMap = false;
        } catch (Exception e) {

        }
    }

    public static void readVersionMapFromFile() {
        ObjectMapper mapper = new ObjectMapper();
        String line = null;
        try {
            File file = new File(RESOURCE_VERSION_PATH);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(reader);
                StringBuffer stringBuffer = new StringBuffer();
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuffer.append(line);
                    stringBuffer.append("\n");
                    versionsMap = mapper.readValue(line, Map.class);
                }
                reader.close();
            } else
                LOGGER.info("{} file doesn't exist, starting with empty versionsMap", RESOURCE_VERSION_PATH);
        } catch (Exception e) {
            LOGGER.error("Error while reading the file resource-version.json file", e);
        }
    }
    
    public static String getDefaultApiVersion(HttpClient httpClient, String url, String resourceType, AuthenticationResult authTokenResult, String loggingPrefix) {
        String defaultApiVersion = null;        
        try {            
            LOGGER.debug("{} - Resource Type ||{}|| API request for the default API version: ||{}||", loggingPrefix, resourceType, url);
            HttpGet request = new HttpGet(url);
            request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());            
            HttpResponse response = httpClient.execute(request);
            LOGGER.debug("{} - Resource Type ||{}|| API response status code for the default API version: ||{}||", loggingPrefix, resourceType, response.getStatusLine().getStatusCode());

            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            LOGGER.trace("{} - Resource Type ||{}|| API response for the default API version: ||{}||", loggingPrefix, resourceType, responseBody);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                try {
                    JSONArray jsonResourceTypesArray = new JSONObject(responseBody).getJSONArray("resourceTypes");
                    if (!jsonResourceTypesArray.isEmpty()) {
                        for (int i = 0; i < jsonResourceTypesArray.length(); i++) {
                            JSONObject resourceTypeObject = jsonResourceTypesArray.getJSONObject(i);                    
                            if (resourceTypeObject.getString("resourceType").equalsIgnoreCase(resourceType)) {
                                defaultApiVersion = resourceTypeObject.getString("defaultApiVersion");
                                break;
                            }                    
                        }
                    }    
                }
                catch(Exception e) {
                    LOGGER.error("{} - Resource Type: ||{}|| - Exception parsing response for default API version. Response: ||{}||", loggingPrefix, resourceType, responseBody, e);
                }                     
            }               
        } catch (Exception e) {
            LOGGER.error("{} - Resource Type: ||{}|| - Exception while querying for default API version.", loggingPrefix, resourceType, e);
        }
        return defaultApiVersion;
    }
}
