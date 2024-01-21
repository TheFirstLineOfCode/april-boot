package com.thefirstlineofcode.april.boot;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.pf4j.BasePluginLoader;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;
import org.pf4j.util.FileUtils;

public class AprilDevelopmentPlguinLoader extends BasePluginLoader {
	public AprilDevelopmentPlguinLoader(PluginManager pluginManager) {
		super(pluginManager, new AprilDevelopmentPluginClasspath());
	}
	
	public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {
		PluginClassLoader pluginClassLoader = new AprilPluginClassLoader(pluginManager, pluginDescriptor,
				getClass().getClassLoader());
		loadClasses(pluginPath, pluginClassLoader);
		loadJars(pluginPath, pluginClassLoader);
		
		return pluginClassLoader;
	}
	
	@Override
	protected void loadJars(Path pluginPath, PluginClassLoader pluginClassLoader) {
		for (String jarsDirectory : pluginClasspath.getJarsDirectories()) {
			Path file = pluginPath.resolve(jarsDirectory);
			List<File> jars = FileUtils.getJars(file);
			for (File jar : jars) {
				if (!isPluginJar(jar))
					pluginClassLoader.addFile(jar);
			}
		}
	}

	private boolean isPluginJar(File jar) {
		try {
			Path pluingProperties = FileUtils.getPath(jar.toPath(), "plugin.properties");
			return Files.exists(pluingProperties);
		} catch (IOException e) {
			return false;
		}
	}
}
