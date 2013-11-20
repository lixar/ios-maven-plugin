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
    static final String DEFAULT_BUILD_CONFIGURATION = "Release";
    static final String DEFAULT_SHARED_PRECOMPS_DIR = "SharedPrecompiledHeaders";

    /**
     * The project currently being built.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Absolute path to the base directory.
     */
    protected String baseDir;
    /**
     * Absolute path to the target directory where artifacts are built.
     */
    protected File targetDir;
    /**
     * Absolute path to the working directory.
     */
    protected File workDir;
    /**
     * Absolute path to the built .app file.
     */
    protected String appDir;

    /**
     * iOS Source Directory
     *
     * @parameter property="ios.sourceDir"
     *            default-value="."
     */
    protected String sourceDir;

    /**
     * iOS app name
     *
     * @parameter property="ios.appName"
     * @required
     */
    protected String appName;

    /**
     * iOS project name
     *
     * @parameter property="ios.projectName"
     */
    protected String projectName;

    /**
     * iOS workspace name
     *
     * @parameter property="ios.workspaceName"
     */
    protected String workspaceName;

    /**
     * iOS scheme
     *
     * @parameter property="ios.scheme"
     */
    protected String scheme;

    /**
     * iOS scheme
     *
     * @parameter property="ios.target"
     */
    protected String target;

    /**
     * iOS sdk
     *
     * @parameter property="ios.sdk"
     */
    protected String sdk;

    /**
     * iOS build configuration
     *
     * @parameter property="ios.buildConfiguration"
     */
    protected String buildConfiguration;

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
        loadDefaults();

        baseDir = project.getBasedir().toString();
        targetDir = new File(project.getBuild().getDirectory());
        workDir = new File(baseDir + File.separator + sourceDir);
        appDir = targetDir + File.separator + buildConfiguration + "-" + sdk + File.separator;

        getLog().debug("Using '" + getBuildCommand() + "' command for building");
    }

    private void loadDefaults() {
        if (null == sourceDir) {
            sourceDir = "";
        }
        if (null == buildConfiguration) {
            buildConfiguration = DEFAULT_BUILD_CONFIGURATION;
        }
        if (null == sdk) {
            sdk = DEFAULT_SDK;
        }
        if (useXctool) {
            useXctool = xctoolExists();
        }
    }

    private String getXctoolPath() {
        ProcessBuilder pb = new ProcessBuilder("which", "xctool");
        String xctoolPath;
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

    protected String getArtifactPath(String extension) {
        if (null == extension) {
            extension = "";
        }
        return appDir + File.separator + project.getBuild().getFinalName() + "." + extension;
    }
}
