/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.utils;

import org.junit.Assert;
import org.junit.Test;

public class AzureApiVersionStoreTest {

    @Test
    public void getLastMatch() {
        String message = "The resource type 'virtualMachines' could not be found in the namespace 'Microsoft.Compute' for api version '2019-01-01'. The supported api-versions are '2015-05-01-preview,2015-06-15,2016-03-30,2016-04-30-preview,2016-08-30,2017-03-30,2017-12-01,2018-04-01,2018-06-01,2018-10-01,2019-03-01,2019-07-01,2019-12-01,2020-06-01'.";
        String result = AzureApiVersionStore.getLastMatch(message);
        Assert.assertEquals(result, "2020-06-01");
    }
}