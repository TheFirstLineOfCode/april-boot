package com.thefirstlineofcode.april.boot;

import org.pf4j.PluginClasspath;

public class AprilDevelopmentPluginClasspath extends PluginClasspath {
	public AprilDevelopmentPluginClasspath() {
		addClassesDirectories("target/classes");
		addJarsDirectories("target/dependency");
	}
}
