package com.thefirstlineofcode.april.boot.config;

import java.util.Properties;

public class PluginProperties implements IPluginProperties {
	protected Properties properties;
	
	public PluginProperties() {
		this(new Properties());
	}
	
	public PluginProperties(Properties properties) {
		this.properties = properties;
	}

	@Override
	public Boolean getBoolean(String propertyName) {
		return getBoolean(propertyName, null);
	}

	@Override
	public Boolean getBoolean(String propertyName, Boolean defaultValue) {
		String value = properties.getProperty(propertyName);
		if (value == null)
			return defaultValue;
		
		return Boolean.parseBoolean(value);
	}

	@Override
	public Integer getInteger(String propertyName) {
		return getInteger(propertyName, null);
	}

	@Override
	public Integer getInteger(String propertyName, Integer defaultValue) {
		String value = properties.getProperty(propertyName);
		if (value == null)
			return defaultValue;
		
		return Integer.parseInt(value);
	}

	@Override
	public String getString(String propertyName) {
		return getString(propertyName, null);
	}

	@Override
	public String getString(String propertyName, String defaultValue) {
		String value = properties.getProperty(propertyName);
		if (value == null)
			return defaultValue;
		
		return value;
	}

	@Override
	public String[] getPropertyNames() {
		Object[] objects = properties.keySet().toArray();
		String[] propertyNames = new String[objects.length];
		
		for (int i = 0; i < objects.length; i++) {
			propertyNames[i] = (String)objects[i];
		}
		
		return propertyNames;
	}
	
	public Properties getProperties() {
		return properties;
	}
}
