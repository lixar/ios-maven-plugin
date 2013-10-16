package com.brewinapps.ios;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


/**
 * @author Brewin' Apps AS
 * @goal package
 * @phase package
 */
public class IOSPackageMojo extends IOSAbstractMojo {

    /**
     * iOS app name
     *
     * @parameter expression="${ios.appName}"
     * @required
     */
    private String appName;

    /**
     * iOS build configuration
     *
     * @parameter expression="${ios.buildConfiguration}"
     */
    private String buildConfiguration;

    private String appDir;
    private String targetDir;


    /**
     *
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initialize();
            validateParameters();

            final String packageName = project.getBuild().getFinalName() + ".zip";
            packageApp(packageName);

            project.getArtifact().setFile(new File(appDir + File.separator + packageName));
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
    }

    protected void validateParameters() throws IOSException {
        if (null == buildConfiguration) {
            buildConfiguration = DEFAULT_BUILD_CONFIGURATION;
        }
    }

    protected void packageApp(String packageName) throws IOSException {
        ProcessBuilder pb = new ProcessBuilder(
                "zip",
                "-r",
                packageName,
                appName + ".app.dSYM",
                project.getBuild().getFinalName() + ".ipa");
        pb.directory(new File(appDir));
        executeCommand(pb);
    }
}
