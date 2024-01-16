package com.thefirstlineofcode.april.boot;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.pf4j.PluginWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.origin.OriginTrackedValue;

import com.thefirstlineofcode.april.boot.config.ConfigurationProperties;
import com.thefirstlineofcode.april.boot.config.IConfigurationProperties;
import com.thefirstlineofcode.april.boot.config.IConfigurationPropertiesAware;

public class AprilBeanPostProcessor implements BeanPostProcessor {
	private static final String APRIL_PLUGINS_PREFIX = "april.plugins";
	
	private Path applicationHome;
	private Map<String, Object> applicationProperties;
	private AprilPluginManager pluginManager;
	private Map<String, IConfigurationProperties> pluginIdToConfigurationProperties;

	
	public AprilBeanPostProcessor(Path applicationHome, Map<String, Object> applicationProperties, AprilPluginManager pluginManager) {
		this.applicationHome = applicationHome;
		this.applicationProperties = applicationProperties;
		this.pluginManager = pluginManager;
		
		pluginIdToConfigurationProperties = new HashMap<>();
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof IApplicationHomeAware) {
			((IApplicationHomeAware)bean).setApplicationHome(applicationHome);
		}
		
		if (bean instanceof IPluginManagerAware) {
			((IPluginManagerAware)bean).setPluginManager(pluginManager);
		}
		
		PluginWrapper plugin = pluginManager.loadedBy(bean);
		if (plugin == null)
			return bean;
		
		if (bean instanceof IConfigurationPropertiesAware) {
			((IConfigurationPropertiesAware)bean).setConfigurationProperties(getConfigurationProperties(plugin.getPluginId()));
		}
		
		return bean;
	}

	private IConfigurationProperties getConfigurationProperties(String pluginId) {
		IConfigurationProperties configurationProperties = pluginIdToConfigurationProperties.get(pluginId);
		if (configurationProperties == null) {
			synchronized (applicationProperties) {
				configurationProperties = pluginIdToConfigurationProperties.get(pluginId);
				if (configurationProperties != null)
					return configurationProperties;
				
				String pluginPropertiesPrefix = String.format("%s.%s.", APRIL_PLUGINS_PREFIX, pluginId);
				Properties properties = new Properties();
				Set<String> keys = applicationProperties.keySet();
				for (String key : keys) {
					if (key.startsWith(pluginPropertiesPrefix) && key.length() > pluginPropertiesPrefix.length()) {
						String propertyName = key.substring(pluginPropertiesPrefix.length());
						String propertyValue = ((OriginTrackedValue)applicationProperties.get(key)).getValue().toString();
						
						properties.put(propertyName, propertyValue);
					}
				}
				
				configurationProperties = new ConfigurationProperties(properties);
				pluginIdToConfigurationProperties.put(pluginId, configurationProperties);
			}
		}
		
		return configurationProperties;
	}
}
