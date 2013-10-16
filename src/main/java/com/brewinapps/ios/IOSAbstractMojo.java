package com.brewinapps.ios;

import org.apache.maven.plugin.AbstractMojo;

public abstract class IOSAbstractMojo extends AbstractMojo {

    static final String DEFAULT_SDK = "iphoneos";
    static final String DEFAULT_BUILD_CONFIGURATION = "Adhoc";
    static final String DEFAULT_SHARED_PRECOMPS_DIR = "SharedPrecompiledHeaders";

    protected String executeCommand(ProcessBuilder pb) throws IOSException {
        return CommandHelper.performCommand(pb, getLog());
    }
}
