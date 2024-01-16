package com.thefirstlineofcode.april.boot;

import org.pf4j.ExtensionPoint;

public interface ISchemaInitializer extends ExtensionPoint {
	String getInitialScript();
}
