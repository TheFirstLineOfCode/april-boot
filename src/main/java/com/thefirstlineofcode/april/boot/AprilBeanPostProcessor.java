package com.thefirstlineofcode.april.boot;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.pf4j.PluginWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.thefirstlineofcode.april.boot.config.ApplicationProperties;
import com.thefirstlineofcode.april.boot.config.DummyPluginProperties;
import com.thefirstlineofcode.april.boot.config.IApplicationPropertiesAware;
import com.thefirstlineofcode.april.boot.config.IPluginProperties;
import com.thefirstlineofcode.april.boot.config.IPluginPropertiesAware;
import com.thefirstlineofcode.april.boot.config.PluginProperties;
import com.thefirstlineofcode.april.boot.config.SectionalProperties;

public class AprilBeanPostProcessor implements BeanPostProcessor {
	private Path applicationHome;
	private SectionalProperties aprilProperties;
	private ApplicationProperties applicationProperties;
	private AprilPluginManager pluginManager;
	private Map<String, IPluginProperties> pluginIdToPluginProperties;

	
	public AprilBeanPostProcessor(Path applicationHome, SectionalProperties aprilProperties,
				ApplicationProperties applicationProperties, AprilPluginManager pluginManager) {
		this.applicationHome = applicationHome;
		this.aprilProperties = aprilProperties;
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
		
		if (bean instanceof IApplicationPropertiesAware) {
			((IApplicationPropertiesAware)bean).setApplicationProperties(applicationProperties);
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
			synchronized (pluginIdToPluginProperties) {
				pluginProperties = pluginIdToPluginProperties.get(pluginId);
				if (pluginProperties != null)
					return pluginProperties;
				
				Properties properties = aprilProperties.getSection(pluginId);
				
				if (properties == null)
					pluginProperties = new DummyPluginProperties();
				else
					pluginProperties = new PluginProperties(properties);
				
				pluginIdToPluginProperties.put(pluginId, pluginProperties);
			}
		}
		
		return pluginProperties;
	}
}
