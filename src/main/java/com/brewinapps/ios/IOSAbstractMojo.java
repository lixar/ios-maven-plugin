package com.brewinapps.ios;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Base class with shared configuration.
 *
 * @author Sylvain Guillope
 */
public abstract class IOSAbstractMojo extends AbstractMojo {

    static final String DEFAULT_SDK = "iphoneos";
    static final String DEFAULT_BUILD_CONFIGURATION = "Adhoc";
    static final String DEFAULT_SHARED_PRECOMPS_DIR = "SharedPrecompiledHeaders";
    static final String XCTOOL_PATH = "/usr/local/bin/xctool";

    /**
     * The project currently being built.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;


    protected String executeCommand(ProcessBuilder pb) throws IOSException {
        return CommandHelper.performCommand(pb, getLog());
    }

    protected void initialize() {
    }

}
