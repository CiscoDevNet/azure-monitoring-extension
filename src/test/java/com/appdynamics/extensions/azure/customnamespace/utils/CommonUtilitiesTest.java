/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.utils;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import org.junit.Test;

import java.util.Arrays;

public class CommonUtilitiesTest {

    @Test
    public void checkStringPatternMatchTestPositive() {
        boolean res = CommonUtilities.checkStringPatternMatch("my-resource-group", Arrays.asList("resource.*", "my-resource.*"));
        assertTrue(res);
    }

    @Test
    public void checkStringPatternMatchTestNegative() {
        boolean res = CommonUtilities.checkStringPatternMatch("my-resource-group", Arrays.asList("resource.*", "my-resource", "resource-grp"));
        assertFalse(res);
    }
}