package com.appdynamics.extensions.azure.customnamespace.utils;

import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.AUTHORIZATION;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.BEARER;
import static com.appdynamics.extensions.azure.customnamespace.utils.Constants.RESOURCE_VERSION_PATH;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.microsoft.aad.adal4j.AuthenticationResult;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
public class AzureApiVersionStore {
    static Logger LOGGER = ExtensionsLoggerFactory.getLogger(AzureApiVersionStore.class);
    private static Map<String, String> versionsMap = Maps.newConcurrentMap();
    private static String DATE_REGEX = "(20\\d\\d-\\d\\d-\\d\\d)"; // To match the Azure api date formats
    private static Boolean updateVersionMap = false;

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
                }
                reader.close();
                versionsMap = mapper.readValue(line, Map.class);
            }else
                LOGGER.info("{} file doesn't exist, startting with empty versionsMap", RESOURCE_VERSION_PATH);
        } catch (Exception e) {
            LOGGER.error("Error while reading the file resouce-version.json file", e);
        }
    }


    //TODO: Discuss to make it singleton
    public static String getAptApiVersion(HttpClient httpClient, String url, String version, String resource, AuthenticationResult authTokenResult) {
        String api_version = null;
        if (!versionsMap.containsKey(resource)) {
            api_version = queryApiVersion(httpClient, url + version, version, authTokenResult);
            versionsMap.put(resource, api_version);
            updateVersionMap = true;
        }
        return versionsMap.get(resource);

    }

    // TODO: Devise a better way to do it. Update the api version after reading it from error message
    private static String queryApiVersion(HttpClient httpClient, String url, String version, AuthenticationResult authTokenResult) {
        String api_version = null;
        try {
            HttpGet request = new HttpGet(url);
            request.addHeader(AUTHORIZATION, BEARER + authTokenResult.getAccessToken());
            HttpResponse response1 = httpClient.execute(request);
            String responseBody = EntityUtils.toString(response1.getEntity(), "UTF-8");
            if (new JSONObject(responseBody).isNull("error"))
                return version;
            api_version = extractApiVersion(responseBody);
        } catch (Exception e) {
            LOGGER.error("Exception while querying the resource", e);
        }
        return api_version;
    }

    private static String extractApiVersion(String responseBody) {
        if (((JSONObject) (new JSONObject(responseBody).get("error"))).get("code").equals("InvalidResourceType") || ((JSONObject) (new JSONObject(responseBody).get("error"))).get("code").equals("NoRegisteredProviderFound")) {
            String foundVersion = getLastMatch(((JSONObject) (new JSONObject(responseBody).get("error"))).get("message").toString());
            LOGGER.error("Auto Detect apt version {} for the server API", foundVersion);
            return foundVersion.trim();
        }
        return null;
    }


    public static String getLastMatch(String message) {
        String match = null;
        for (String text : message.split(",")) {
            Matcher m = Pattern.compile(DATE_REGEX).matcher(text);
            if (m.find()) {
                match = m.group();
            }
        }
        return match;
    }
}
