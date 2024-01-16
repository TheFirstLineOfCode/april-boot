package com.thefirstlineofcode.april.boot;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
		ZipInputStream zis = null;
		try {
			zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(jar)));
			ZipEntry entry = null;
			while ((entry = zis.getNextEntry()) != null) {
				String entryName = entry.getName();
				if ("plugin.properties".equals(entryName))
					return true;
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to read jar entries.", e);
		} finally {
			if (zis != null)
				try {
					zis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
		return false;
	}
}
