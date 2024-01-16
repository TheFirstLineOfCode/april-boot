package com.thefirstlineofcode.april.boot;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.pf4j.ClassLoadingStrategy;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AprilPluginClassLoader extends PluginClassLoader {
	private static final Logger log = LoggerFactory.getLogger(AprilPluginClassLoader.class);
	
	public AprilPluginClassLoader(PluginManager pluginManager, PluginDescriptor pluginDescriptor, ClassLoader parent) {
		super(pluginManager, pluginDescriptor, parent, ClassLoadingStrategy.APD);
	}
	
	public AprilPluginClassLoader(PluginManager pluginManager, PluginDescriptor pluginDescriptor, ClassLoader parent,
			ClassLoadingStrategy classLoadingStrategy) {
		super(pluginManager, pluginDescriptor, parent, classLoadingStrategy);
	}
	
	// Always use ClassLoadingStrategy.PAD to get resources.
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> resources = new ArrayList<>();

        log.trace("Received request to load resources '{}'", name);
        for (ClassLoadingStrategy.Source classLoadingSource : ClassLoadingStrategy.PAD.getSources()) {
            switch (classLoadingSource) {
                case APPLICATION:
                    if (getParent() != null) {
                        resources.addAll(Collections.list(getParent().getResources(name)));
                    }
                    break;
                case PLUGIN:
                    resources.addAll(Collections.list(findResources(name)));
                    break;
                case DEPENDENCIES:
                    resources.addAll(findResourcesFromDependencies(name));
                    break;
            }
        }

        return Collections.enumeration(resources);
    }
    
	// Always use ClassLoadingStrategy.PAD to get resource.
    @Override
    public URL getResource(String name) {
        log.trace("Received request to load resource '{}'", name);
        for (ClassLoadingStrategy.Source classLoadingSource : ClassLoadingStrategy.PAD.getSources()) {
            URL url = null;
            switch (classLoadingSource) {
                case APPLICATION:
                    url = super.getResource(name);
                    break;
                case PLUGIN:
                    url = findResource(name);
                    break;
                case DEPENDENCIES:
                    url = findResourceFromDependencies(name);
                    break;
            }

            if (url != null) {
                log.trace("Found resource '{}' in {} classpath", name, classLoadingSource);
                return url;
            } else {
                log.trace("Couldn't find resource '{}' in {}", name, classLoadingSource);
            }
        }

        return null;
    }
}
