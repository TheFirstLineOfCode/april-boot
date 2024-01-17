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

import com.thefirstlineofcode.april.boot.config.PluginProperties;
import com.thefirstlineofcode.april.boot.config.IPluginProperties;
import com.thefirstlineofcode.april.boot.config.IPluginPropertiesAware;

public class AprilBeanPostProcessor implements BeanPostProcessor {
	private static final String APRIL_PLUGINS_PREFIX = "april.plugins";
	
	private Path applicationHome;
	private Map<String, Object> applicationProperties;
	private AprilPluginManager pluginManager;
	private Map<String, IPluginProperties> pluginIdToPluginProperties;

	
	public AprilBeanPostProcessor(Path applicationHome, Map<String, Object> applicationProperties, AprilPluginManager pluginManager) {
		this.applicationHome = applicationHome;
		this.applicationProperties = applicationProperties;
		this.pluginManager = pluginManager;
		
		pluginIdToPluginProperties = new HashMap<>();
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
		
		if (bean instanceof IPluginPropertiesAware) {
			((IPluginPropertiesAware)bean).setPluginProperties(getPluginProperties(plugin.getPluginId()));
		}
		
		return bean;
	}

	private IPluginProperties getPluginProperties(String pluginId) {
		IPluginProperties pluginProperties = pluginIdToPluginProperties.get(pluginId);
		if (pluginProperties == null) {
			synchronized (applicationProperties) {
				pluginProperties = pluginIdToPluginProperties.get(pluginId);
				if (pluginProperties != null)
					return pluginProperties;
				
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
				
				pluginProperties = new PluginProperties(properties);
				pluginIdToPluginProperties.put(pluginId, pluginProperties);
			}
		}
		
		return pluginProperties;
	}
}
