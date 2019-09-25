/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.config;

import com.google.common.collect.Maps;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class Stat {
    @XmlAttribute
    private String resourceType;
    @XmlAttribute
    private String service;
    @XmlElement(name = "metric")
    private MetricConfig[] metricConfig;

    public MetricConfig[] getMetricConfig() {
        return metricConfig;
    }

    public void setMetricConfig(MetricConfig[] metricConfig) {
        this.metricConfig = metricConfig;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }


    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Stats {
        private static Map<String, Stat> serviceStatsMap;
        @XmlElement(name = "stat")
        private Stat[] stats;

        public Stat getStats(String service) {
            if (serviceStatsMap == null)
                buildServiceStatsMap();
            return serviceStatsMap.get(service);
        }

        public void setStats(Stat[] stats) {
            this.stats = stats;
        }

        public void buildServiceStatsMap() {
            serviceStatsMap = Maps.newHashMap();
            for (Stat stat : stats) {
                String service = stat.getService();
                serviceStatsMap.put(service, stat);
            }
        }


    }
}
