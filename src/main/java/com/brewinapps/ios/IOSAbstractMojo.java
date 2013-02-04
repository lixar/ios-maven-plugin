package com.brewinapps.ios;

import org.apache.maven.plugin.AbstractMojo;

public abstract class IOSAbstractMojo extends AbstractMojo {

	static final String DEFAULT_SDK = "iphoneos";
	static final String DEFAULT_BUILD_CONFIGURATION = "Adhoc";
	
	protected void executeCommand(ProcessBuilder pb) throws IOSException {
		CommandHelper.performCommand(pb, getLog());
	}
}
