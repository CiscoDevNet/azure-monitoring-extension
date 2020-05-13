/*
 *
 *  * Copyright 2018. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.azure.customnamespace.config;

/**
 * @author Prashant Mehta
 *
 */
public class MetricsTimeRange {
	
	private int startTimeInMinsBeforeNow;
	
	private int endTimeInMinsBeforeNow;

	public int getStartTimeInMinsBeforeNow() {
		return startTimeInMinsBeforeNow;
	}

	public void setStartTimeInMinsBeforeNow(int startTimeInMinsBeforeNow) {
		this.startTimeInMinsBeforeNow = startTimeInMinsBeforeNow;
	}

	public int getEndTimeInMinsBeforeNow() {
		return endTimeInMinsBeforeNow;
	}

	public void setEndTimeInMinsBeforeNow(int endTimeInMinsBeforeNow) {
		this.endTimeInMinsBeforeNow = endTimeInMinsBeforeNow;
	}

}
