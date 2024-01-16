package com.thefirstlineofcode.april.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("april.application")
public class AprilApplicationProperties {
	private String[] disabledPlugins;

	public String[] getDisabledPlugins() {
		return disabledPlugins;
	}

	public void setDisabledPlugins(String[] disabledPlugins) {
		this.disabledPlugins = disabledPlugins;
	}
}
