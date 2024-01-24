package com.thefirstlineofcode.april.boot;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.core.env.ConfigurableEnvironment;

import com.thefirstlineofcode.april.boot.config.ApplicationProperties;
import com.thefirstlineofcode.april.boot.config.AprilUtils;
import com.thefirstlineofcode.april.boot.config.SectionalProperties;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class AprilFlowers {
	private static final String SECTION_NAME_APPLICATION = "application";
	private static final String FILE_NAME_APRIL_PROPERTIES = "april.properties";
	private static final String PROPERTY_NAME_LOG_LEVEL = "logLevel";
	private static final String PROPERTY_NAME_DISABLED_PLUGINS = "disabledPlugins";
	private static final String OPTION_NAME_PLUGINS_DIRECTORIES = "plugins-directories";
	private static final String DEFAULT_PLUGINS_DIRECTORY_NAME = "plugins";
	private static final String OPTION_NAME_RUNTIME_MODE = "runtime-mode";
	
	private Path applicationHome;
	private Path[] pluginsDirectories;
	
	private SectionalProperties aprilProperties;
	private ApplicationProperties applicationProperties;
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
		if (noPlugins)
			return;
				
		System.setProperty(AbstractPluginManager.MODE_PROPERTY_NAME, runtimeMode.toString());
		AprilPluginManager pluginManager = new AprilPluginManager(pluginsDirectories);
		pluginManager.loadPlugins();
		
		if (disabledPlugins != null) {			
			for (String disabledPlugin : disabledPlugins) {
				PluginWrapper plugin = pluginManager.getPlugin(disabledPlugin);
				if (plugin == null)
					throw new RuntimeException(String.format("Can't disable plugin '%s'. The plugin doesn't exist.", disabledPlugin));
				
				plugin.setPluginState(PluginState.DISABLED);
			}
		}
		
		pluginManager.startPlugins();
		
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory)appContext.getBeanFactory();
		beanFactory.addBeanPostProcessor(new AprilBeanPostProcessor(applicationHome, aprilProperties, applicationProperties, pluginManager));
		
		AnnotationConfigRegistry configRegistry = (AnnotationConfigRegistry)appContext;
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
	
	protected void processEnvironment(ConfigurableEnvironment environment) {
		applicationProperties = new ApplicationProperties();
		
		Path pAprilProperties = applicationHome.resolve(FILE_NAME_APRIL_PROPERTIES);
		aprilProperties = new SectionalProperties();
		if (Files.exists(pAprilProperties)) {
			try {
				aprilProperties.load(new FileInputStream(pAprilProperties.toFile()));
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed load properties from %s.",
						pAprilProperties.toFile().getAbsolutePath()), e);
			}
			
			Properties appProperties = aprilProperties.getSection(SECTION_NAME_APPLICATION);
			configureApplicationProperties(applicationProperties, appProperties);
		}
		
		disabledPlugins = applicationProperties.getDisabledPlugins();
		configureLog(applicationProperties.getLogLevel());
	}

	private void configureApplicationProperties(ApplicationProperties applicationProperties,
			Properties appProperties) {
		if (appProperties == null)
			return;
		
		String disabledPlugins = appProperties.getProperty(PROPERTY_NAME_DISABLED_PLUGINS);
		if (disabledPlugins != null)
			applicationProperties.setDisabledPlugins(AprilUtils.stringToArray(disabledPlugins));
		
		String logLevel = appProperties.getProperty(PROPERTY_NAME_LOG_LEVEL);
		if (logLevel != null)
			applicationProperties.setLogLevel(logLevel);
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
			// Ignore. StatusPrinter will handle this.
		}
		
	    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
	}
}
