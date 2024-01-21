package com.thefirstlineofcode.april.boot;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.pf4j.JarPluginLoader;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AprilPluginLoader extends JarPluginLoader {
	private static final Logger logger = LoggerFactory.getLogger(AprilPluginLoader.class);
	
	public AprilPluginLoader(AprilPluginManager pluginManager) {
		super(pluginManager);
	}
	
	@Override
	public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {		
		File pluginDependenciesDirectory = getPluginDependenciesDirectory(pluginPath, pluginDescriptor);		
		if (pluginDependenciesDirectory == null) {
			logger.info("Plugin dependencies directory not found. Use default plugin classloader to load plugin.");
			return super.loadPlugin(pluginPath, pluginDescriptor);
		}
		
		File[] pluginDependencies = getPluginDependencies(pluginDependenciesDirectory);
		if (pluginDependencies == null) {
			logger.warn("Found plugin dependencies dirctory, but no plugin dependencies found. Use default plugin classloader to load plugin.");			
			return super.loadPlugin(pluginPath, pluginDescriptor);
		}
		
		PluginClassLoader pluginClassLoader = new AprilPluginClassLoader(pluginManager, pluginDescriptor,
				getClass().getClassLoader());
		pluginClassLoader.addFile(pluginPath.toFile());
		for (File pluginDependency : pluginDependencies) {
			pluginClassLoader.addFile(pluginDependency);
		}
		
		return pluginClassLoader;
	}

	protected File[] getPluginDependencies(File pluginDependenciesDirectory) {
		File[] children = pluginDependenciesDirectory.listFiles();
		
		List<File> jars = new ArrayList<>();
		for (File child : children) {
			if (FileUtils.isJarFile(child.toPath()))
				jars.add(child);
		}
		
		return jars.size() != 0 ? jars.toArray(new File[jars.size()]) : null;
	}

	protected File getPluginDependenciesDirectory(Path pluginPath, PluginDescriptor pluginDescriptor) {
		String sPluginDependenciesDirectory = String.format("%s-dependencies",
				pluginDescriptor.getPluginId());
		Path pPluginDependenciesDirectory = pluginPath.resolveSibling(sPluginDependenciesDirectory);
		
		File pluginDependenciesDirectory = pPluginDependenciesDirectory.toFile();
		if (pluginDependenciesDirectory.exists() && pluginDependenciesDirectory.isDirectory())
			return pluginDependenciesDirectory;
		
		return null;
	}
}	
