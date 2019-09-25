/*
 *
 *  * Copyright 2019. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.config;


import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class CredentialsDecryptionConfig {

    private String enableDecryption;

    private String encryptionKey;

    public boolean isDecryptionEnabled() {
        return Boolean.valueOf(getEnableDecryption());
    }

    public String getEnableDecryption() {
        return enableDecryption;
    }

    public void setEnableDecryption(String enableDecryption) {
        this.enableDecryption = enableDecryption;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
