package com.thefirstlineofcode.april.boot;

import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.pf4j.AbstractPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.RuntimeMode;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class AprilFlowers {
	private static final String PROPERTY_NAME_APRIL_APPLICATION_LOG_LEVEL = "april.application.logLevel";
	private static final String PROPERTY_NAME_APRIL_APPLICATION_DISABLED_PLUGINS = "april.application.disabledPlugins";
	private static final String OPTION_NAME_PLUGINS_DIRECTORIES = "plugins-directories";
	private static final String DEFAULT_PLUGINS_DIRECTORY_NAME = "plugins";
	private static final String OPTION_NAME_RUNTIME_MODE = "runtime-mode";
	
	private Path applicationHome;
	private Path[] pluginsDirectories;
	
	private Map<String, Object> applicationProperties;
	private boolean noPlugins;
	private RuntimeMode runtimeMode;
	private String[] disabledPlugins;
	
	public AprilFlowers(SpringApplication application, String[] args) {
		if (!(application instanceof AprilApplicationImpl))
			throw new RuntimeException(String.format("'%s' must be a AprilApplicationImpl instance.", application));
		
		applicationHome = ((AprilApplicationImpl)application).getApplicationHome();
		
		noPlugins = false;
		ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
		List<String> pluginsDirectoriesOptionValues = applicationArguments.getOptionValues(OPTION_NAME_PLUGINS_DIRECTORIES);
		
		if (pluginsDirectoriesOptionValues == null) {
			try {
				pluginsDirectories = new Path[] {applicationHome.resolve(DEFAULT_PLUGINS_DIRECTORY_NAME)};				
			} catch (InvalidPathException e) {
				pluginsDirectories = null;
			}
		} else {
			pluginsDirectories = getPluginsDirectories(pluginsDirectoriesOptionValues);
		}
		
		if (pluginsDirectories == null) {
			noPlugins = true;
			return;
		}
		
		for (Path pluginsDirectory : pluginsDirectories) {			
			if (!pluginsDirectory.toFile().exists()) {
				if (pluginsDirectoriesOptionValues == null) {
					noPlugins = true;
					return;
				}
				
				throw new RuntimeException(String.format("Error: Plugins directory '%s' doesn't exist.", pluginsDirectory.toFile().getAbsolutePath()));
			}
			
			if (!pluginsDirectory.toFile().isDirectory())
				throw new RuntimeException(String.format("Error: Plugins directory '%s' isn't a directory.", pluginsDirectory.toFile().getAbsolutePath()));
		}
		
		
		List<String> runtimeModeOptionValues = applicationArguments.getOptionValues(OPTION_NAME_RUNTIME_MODE);
		if (runtimeModeOptionValues != null) {
			String sRuntimeMode = runtimeModeOptionValues.get(0);
			runtimeMode = RuntimeMode.byName(sRuntimeMode);
		} else {
			runtimeMode = RuntimeMode.DEPLOYMENT;
		}
	}
	
	private Path[] getPluginsDirectories(List<String> sPluginDirectories) {
		List<Path> pluginsDirectories = new ArrayList<>();
		for (int i = 0; i < sPluginDirectories.size(); i++) {
			pluginsDirectories.addAll(getPluginDirectories(sPluginDirectories.get(i)));
		}
		
		return pluginsDirectories.toArray(new Path[pluginsDirectories.size()]);
	}

	private List<Path> getPluginDirectories(String pluginDirectories) {
		StringTokenizer st = new StringTokenizer(pluginDirectories, ",");
		
		List<Path> paths = new ArrayList<>();
		while (st.hasMoreTokens()) {
			paths.add(Path.of(st.nextToken()));
		}
		
		return paths;
	}

	public void bloom(ConfigurableApplicationContext appContext) {
		processEnvironment(appContext.getEnvironment());
		processApplicationContext(appContext);
	}

	private void processApplicationContext(ConfigurableApplicationContext appContext) {
		AnnotationConfigRegistry configRegistry = (AnnotationConfigRegistry)appContext;
		registerSystemConfigurations(configRegistry);
		
		if (noPlugins)
			return;
				
		System.setProperty(AbstractPluginManager.MODE_PROPERTY_NAME, runtimeMode.toString());
		AprilPluginManager pluginManager = new AprilPluginManager(pluginsDirectories);
		pluginManager.loadPlugins();
		
		for (String disabledPlugin : disabledPlugins) {
			PluginWrapper plugin = pluginManager.getPlugin(disabledPlugin);
			if (plugin == null)
				throw new RuntimeException(String.format("Can't disable plugin '%s'. The plugin doesn't exist.", disabledPlugin));
			
			plugin.setPluginState(PluginState.DISABLED);
		}
		
		pluginManager.startPlugins();
		
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory)appContext.getBeanFactory();
		beanFactory.addBeanPostProcessor(new AprilBeanPostProcessor(applicationHome, applicationProperties, pluginManager));
		
		ClassLoader[] pluginClassLoaders = registerSpringConfigurations(configRegistry, pluginManager);
		if (pluginClassLoaders != null) {
			appContext.setClassLoader(new CompositeClassLoader(getNewAppContextClassLoaders(appContext, pluginClassLoaders)));
		}
	}

	private ClassLoader[] getNewAppContextClassLoaders(ConfigurableApplicationContext appContext, ClassLoader[] pluginClassLoaders) {
		ClassLoader[] newAppContextClassLoaders = new ClassLoader[pluginClassLoaders.length + 1];
		newAppContextClassLoaders[0] = appContext.getClassLoader();
		for (int i = 0; i < pluginClassLoaders.length; i++) {
			newAppContextClassLoaders[i + 1] = pluginClassLoaders[i];
		}
		
		return newAppContextClassLoaders;
	}
	
	protected ClassLoader[] registerSpringConfigurations(AnnotationConfigRegistry configRegistry,
				PluginManager pluginManager) {
		registerPredefinedSpringConfigurations(configRegistry);
		
		return registerContributedSpringConfigurations(configRegistry, pluginManager);
	}
	
	private ClassLoader[] registerContributedSpringConfigurations(AnnotationConfigRegistry configRegistry,
			PluginManager pluginManager) {
		List<Class<? extends ISpringConfiguration>> contributedSpringConfigurationClasses =
				pluginManager.getExtensionClasses(ISpringConfiguration.class);
		if (contributedSpringConfigurationClasses == null || contributedSpringConfigurationClasses.size() == 0)
			return null;
		
		List<ClassLoader> classLoaders = new ArrayList<>();
		for (Class<? extends ISpringConfiguration> contributedSpringConfigurationClass : contributedSpringConfigurationClasses) {			
			configRegistry.register(contributedSpringConfigurationClasses.toArray(
					new Class<?>[contributedSpringConfigurationClasses.size()]));
			
			classLoaders.add(contributedSpringConfigurationClass.getClassLoader());
		}
		
		return classLoaders.toArray(new ClassLoader[classLoaders.size()]);
	}
	
	protected void registerPredefinedSpringConfigurations(AnnotationConfigRegistry configRegistry) {}
	
	protected void registerSystemConfigurations(AnnotationConfigRegistry configRegistry) {
		configRegistry.register(BootConfiguration.class);
	}
	
	protected void processEnvironment(ConfigurableEnvironment environment) {
		Iterator<PropertySource<?>> propertySourcesIter  = environment.getPropertySources().iterator();
		while (propertySourcesIter.hasNext()) {
			PropertySource<?> propertySource = propertySourcesIter.next();
			if (propertySource.getName().indexOf("application.properties") != -1) {
				if (!(propertySource instanceof MapPropertySource))
					throw new RuntimeException("Application properties source isn't a MapPropertySource!!!");
				
				applicationProperties= ((MapPropertySource)propertySource).getSource();
				break;
			}
		}
		
		if (applicationProperties == null)
			return;
		
		String sDisabledPlugins = getPropertyValue(PROPERTY_NAME_APRIL_APPLICATION_DISABLED_PLUGINS);
		if (sDisabledPlugins == null) {			
			disabledPlugins = new String[0];
		} else {			
			disabledPlugins = sDisabledPlugins.split(",");
			for (int i = 0; i < disabledPlugins.length; i++) {
				disabledPlugins[i] = disabledPlugins[i].trim();
			}
		}
		
		String logLevel = getPropertyValue(PROPERTY_NAME_APRIL_APPLICATION_LOG_LEVEL);
		configureLog(logLevel);
	}

	private String getPropertyValue(String key) {
		OriginTrackedValue value = (OriginTrackedValue)applicationProperties.get(key);
		if (value == null)
			return null;
		
		return (String)value.getValue();
	}
	
	private void configureLog(String logLevel) {
		LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
		
		if (logLevel != null) {			
			if ("debug".equals(logLevel)) {
				configureLog(lc, "logback_debug.xml");
			} else if ("trace".equals(logLevel)) {
				configureLog(lc, "logback_trace.xml");
			} else if ("info".equals(logLevel)) {
				configureLog(lc, "logback.xml");
			} else {
				throw new IllegalArgumentException("Unknown log level option. Only 'info', 'debug' or 'trace' is supported.");
			}
		} else {
			configureLog(lc, "logback.xml");
		}
	}

	private void configureLog(LoggerContext lc, String logFile) {
		configureLC(lc, getClass().getClassLoader().getResource(logFile));
	}

	private void configureLC(LoggerContext lc, URL url) {
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			lc.reset();
			configurator.setContext(lc);
			configurator.doConfigure(url);
		} catch (JoranException e) {
			// ignore, StatusPrinter will handle this
		}
		
	    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
	}
}
