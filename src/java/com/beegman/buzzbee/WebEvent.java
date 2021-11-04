/*
 *  Copyright D. Rogatkin 2017-2021
 */
/**
 * Event
 */
package com.beegman.buzzbee;

import java.util.Arrays;

public class WebEvent implements Note {
	public String getResourceId() {
		return resourceId;
	}

	public String resourceId;
	public String action;
	public Object[] attributes;

	public WebEvent setAction(String anaction) {
		action = anaction;
		return this;
	}

	public WebEvent setAttributes(Object...attrs) {
		attributes = attrs;
		return this;
	}
	public WebEvent setId(String id) {
		resourceId = id;
		return this;
	}

	@Override
	public String toString() {
		return "WebEvent [resourceId=" + resourceId + ", action=" + action + ", attributes="
				+ Arrays.toString(attributes) + "]";
	}
	
}
