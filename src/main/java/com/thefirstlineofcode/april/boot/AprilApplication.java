package com.thefirstlineofcode.april.boot;

import java.net.URL;
import java.nio.file.Path;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ResourceLoader;

public class AprilApplication {
	private static final String PROPERTY_APRIL_LOGS_DIR = "april.logs.dir";
	private static final String DIRECTORY_NAME_LOGS = "logs";
	
	private ResourceLoader resourceLoader;
	private Class<?>[] primarySources;
	
	public AprilApplication(Class<?>... primarySources) {
		this(null, primarySources);
	}
	
	public AprilApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
		this.resourceLoader = resourceLoader;
		this.primarySources = primarySources;
	}

	public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
		return run(new Class<?>[] {primarySource}, args);
	}
	
	public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
		return new AprilApplication(primarySources).run(args);
	}
	
	public ConfigurableApplicationContext run(String... args) {
		Path applicationHome = getAprilApplicationHome();
		System.setProperty(PROPERTY_APRIL_LOGS_DIR, applicationHome.resolve(DIRECTORY_NAME_LOGS).toString());
		
		return new AprilApplicationImpl(applicationHome, resourceLoader, primarySources).run(args);
	}
	
	public Path getAprilApplicationHome() {
		URL applicationUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
		String applicationUrlPath = applicationUrl.getPath();
		int dotJarPortStart = applicationUrlPath.indexOf(".jar!/BOOT-INF");
		
		return Path.of(applicationUrlPath.substring("file:/".length(), dotJarPortStart + 4)).getParent();
	}
}
