package com.brewinapps.ios;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


/**
 * @author Brewin' Apps AS
 * @goal package
 * @phase package
 */
public class IOSPackageMojo extends IOSAbstractMojo {
    /**
     * iOS Source Directory
     *
     * @parameter property="ios.sourceDir"
     *            default-value="."
     */
    private String sourceDir;

    /**
     * iOS app name
     *
     * @parameter property="ios.appName"
     * @required
     */
    private String appName;

    /**
     * iOS sdk
     *
     * @parameter property="ios.sdk"
     */
    private String sdk;

    /**
     * iOS code sign identity
     *
     * @parameter property="ios.codeSignIdentity"
     */
    private String codeSignIdentity;

    /**
     * iOS build configuration
     *
     * @parameter property="ios.buildConfiguration"
     */
    private String buildConfiguration;

    private String appDir;
    private String targetDir;
    private File workDir;
    private String baseDir;


    /**
     *
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initialize();
            validateParameters();
            xcrun();
            packageDsym();

            final String artifactName = project.getBuild().getFinalName() + ".ipa";
            project.getArtifact().setFile(new File(appDir + File.separator + artifactName));
        } catch (IOSException e) {
            getLog().error(e.getMessage());
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (Exception e) {
            getLog().error(e.getMessage());
            throw new MojoFailureException(e.getMessage());
        }
    }

    @Override
    protected void initialize() {
        super.initialize();

        targetDir = project.getBuild().getDirectory();
        appDir = targetDir + File.separator + buildConfiguration + "-" + DEFAULT_SDK + File.separator;
        baseDir = project.getBasedir().toString();
        workDir = new File(baseDir + File.separator + sourceDir);

        if (null == sdk) {
            sdk = DEFAULT_SDK;
        }
    }

    protected void validateParameters() throws IOSException {
        if (null == buildConfiguration) {
            buildConfiguration = DEFAULT_BUILD_CONFIGURATION;
        }
    }

    protected void packageDsym() throws IOSException {
        ProcessBuilder pb = new ProcessBuilder(
                "zip",
                "-r",
                project.getBuild().getFinalName() + ".dSYM.zip",
                appDir + appName + ".app.dSYM");
        pb.directory(new File(appDir));
        executeCommand(pb);
    }

    protected void xcrun() throws IOSException {
        List<String> parameters = createXcrunParameters();

        ProcessBuilder pb = new ProcessBuilder(parameters);
        pb.directory(workDir);
        executeCommand(pb);
    }

    protected List<String> createXcrunParameters() {
        List<String> parameters = new ArrayList<String>();
        parameters.add("xcrun");

        getLog().debug("sdk: " + sdk);
        getLog().debug("appDir: " + appDir);
        getLog().debug("appName: " + appName);
        getLog().debug("project.getBuild().getFinalName(): " + project.getBuild().getFinalName());

        parameters.add("-sdk");
        parameters.add(sdk);
        parameters.add("PackageApplication");
        parameters.add("-v");
        parameters.add(appDir + appName + ".app");
        parameters.add("-o");
        parameters.add(appDir + project.getBuild().getFinalName() + ".ipa");

        if (codeSignIdentity != null && codeSignIdentity.length() > 0) {
            parameters.add("--sign");
            parameters.add(codeSignIdentity);
        }

        return parameters;
    }
}
