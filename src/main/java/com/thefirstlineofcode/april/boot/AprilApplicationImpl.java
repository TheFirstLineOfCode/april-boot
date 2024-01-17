package com.thefirstlineofcode.april.boot;

import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ResourceLoader;

public class AprilApplicationImpl extends SpringApplication {
	private Path applicationHome;
	private AprilFlowers flowers;
	
	public AprilApplicationImpl(Path applicationHome, Class<?>... primarySources) {
		this(applicationHome, null, primarySources);
	}
	
	public AprilApplicationImpl(Path applicationHome, ResourceLoader resourceLoader, Class<?>... primarySources) {
		super(resourceLoader, primarySources);
		
		this.applicationHome = applicationHome;
	}

	public static ConfigurableApplicationContext run(Path applicationHome, Class<?> primarySource, String... args) {
		return run(applicationHome, new Class<?>[] {primarySource}, args);
	}
	
	public static ConfigurableApplicationContext run(Path applicationHome, Class<?>[] primarySources, String[] args) {
		return new AprilApplicationImpl(applicationHome, primarySources).run(args);
	}
	
	@Override
	public ConfigurableApplicationContext run(String... args) {
		flowers = new AprilFlowers(this, args);
		
		return super.run(args);
	}
	
	public Path getApplicationHome() {
		return applicationHome;
	}
	
	protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
		super.postProcessApplicationContext(context);
		flowers.bloom(context);
	}
}
