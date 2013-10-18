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

    /**
     * If xctool should be used in lieu of xcodebuild when available on the system
     * See https://github.com/facebook/xctool
     * xctool is expected to be found at /usr/local/bin/xctool
     *
     * @parameter
     * 		property="ios.useXctool"
     * 		default-value="true"
     */
    protected boolean useXctool;

    protected String executeCommand(ProcessBuilder pb) throws IOSException {
        return CommandHelper.performCommand(pb, getLog());
    }

    protected void initialize() {
        if (useXctool) {
            useXctool = xctoolExists();
        }

        getLog().debug("Using '" + getBuildCommand() + "' command for building");
    }

    private String getXctoolPath() {
        ProcessBuilder pb = new ProcessBuilder("which", "xctool");
        String xctoolPath = null;
        try {
            xctoolPath = CommandHelper.performCommand(pb, getLog());
        } catch (IOSException e) {
            return null;
        }

        return xctoolPath;
    }

    private boolean xctoolExists() {
        boolean exists = false;
        String xctoolPath = getXctoolPath();
        if (xctoolPath != null) {
            getLog().debug("xctool found at path '" + xctoolPath + "'");
            File xctoolFile = new File(xctoolPath);
            exists = xctoolFile.canExecute();
        }

        return exists;
    }

    protected String getBuildCommand() {
        return useXctool ? "xctool" : "xcodebuild";
    }
}
