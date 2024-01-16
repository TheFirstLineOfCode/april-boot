package com.thefirstlineofcode.april.boot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.pf4j.CompoundPluginDescriptorFinder;
import org.pf4j.CompoundPluginLoader;
import org.pf4j.DefaultPluginLoader;
import org.pf4j.DefaultPluginManager;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginLoader;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginWrapper;
import org.pf4j.PropertiesPluginDescriptorFinder;
import org.pf4j.RuntimeMode;
import org.pf4j.util.FileUtils;

public class AprilPluginManager extends DefaultPluginManager {
	public AprilPluginManager(Path... pluginsRoots) {
		super(pluginsRoots);
	}
	
	@Override
	protected PluginLoader createPluginLoader() {
		return new CompoundPluginLoader().
				add(new AprilDevelopmentPlguinLoader(this), this::isDevelopment).
				add(new AprilPluginLoader(this), this::isNotDevelopment).
				add(new DefaultPluginLoader(this), this::isNotDevelopment);
	}
		
	@Override
	protected PluginDescriptorFinder createPluginDescriptorFinder() {
		return new CompoundPluginDescriptorFinder()
				.add(new MavenDevelopmentProjectPropertiesPluginDescriptorFinder())
				.add(new ManifestPluginDescriptorFinder());
	}
	
	private class MavenDevelopmentProjectPropertiesPluginDescriptorFinder extends PropertiesPluginDescriptorFinder {
		protected Path getPropertiesPath(Path pluginPath, String propertiesFileName) {
			if (Files.isDirectory(pluginPath)) {
				Path propertiesPath = pluginPath.resolve(Paths.get(propertiesFileName));
				if (RuntimeMode.DEPLOYMENT == runtimeMode || Files.exists(propertiesPath))
					return propertiesPath;
				
				if (RuntimeMode.DEVELOPMENT != runtimeMode)
					throw new RuntimeException("Properties plugin descripotor not found.");
				
				propertiesPath = pluginPath.resolve(Paths.get("target", "classes", propertiesFileName));
				if (Files.notExists(propertiesPath))
					throw new RuntimeException("Properties plugin descripotor not found.");
				
				return propertiesPath;
			} else {
				// it's a jar file
				try {
					return FileUtils.getPath(pluginPath, propertiesFileName);
				} catch (IOException e) {
					throw new PluginRuntimeException(e);
				}
			}
		}
	}
	
	public PluginWrapper loadedBy(Object bean) {
		ClassLoader classLoader = bean.getClass().getClassLoader();
		for (PluginWrapper pluginWrapper : getStartedPlugins()) {
			if (pluginWrapper.getPluginClassLoader().equals(classLoader))
				return pluginWrapper;
		}
		
		return null;
	}
}
