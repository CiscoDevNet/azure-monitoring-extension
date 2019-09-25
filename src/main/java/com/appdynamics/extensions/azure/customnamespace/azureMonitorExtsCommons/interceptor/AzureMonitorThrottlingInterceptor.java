package com.appdynamics.extensions.azure.customnamespace.azureMonitorExtsCommons.interceptor;

import com.microsoft.azure.management.resources.fluentcore.utils.ResourceManagerThrottlingInterceptor;
import okhttp3.Response;

import java.io.IOException;

/*
 Copyright 2019. AppDynamics LLC and its affiliates.
 All Rights Reserved.
 This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 The copyright notice above does not evidence any actual or intended publication of such source code.
*/
//TODO: needed if we want a custom interceptor
public class AzureMonitorThrottlingInterceptor extends ResourceManagerThrottlingInterceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {

        return null;
    }
}
