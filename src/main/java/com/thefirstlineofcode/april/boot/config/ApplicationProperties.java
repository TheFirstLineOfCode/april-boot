package com.thefirstlineofcode.april.boot.config;

public class ApplicationProperties {
	private String logLevel;
	private String[] disabledPlugins;
	
	public ApplicationProperties() {
		logLevel = "info";
		disabledPlugins = new String[0];
	}
	
	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public String[] getDisabledPlugins() {
		return disabledPlugins;
	}

	public void setDisabledPlugins(String[] disabledPlugins) {
		this.disabledPlugins = disabledPlugins;
	}
}
