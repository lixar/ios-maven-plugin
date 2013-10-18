package com.brewinapps.ios;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


/**
 * @author Sylvain Guillope
 * @goal update-version
 */
public class IOSUpdateVersion extends IOSAbstractMojo {

    /**
     * iOS Source Directory
     *
     * @parameter property="ios.sourceDir"
     *            default-value="."
     */
    private String sourceDir;

    /**
     * iOS version
     *
     * @parameter property="ios.version"
     *            default-value="project.version"
     */
    private String version;

    /**
     * Build number
     *
     * @parameter property="ios.buildNumber"
     */
    private String buildNumber;

    /**
     * If the build number should be incremented
     *
     * @parameter property="ios.incrementBuildNumber"
     *            default-value=false
     */
    private boolean incrementBuildNumber;

    private File workDir;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initialize();
            updateVersion();
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

        String baseDir = project.getBasedir().toString();
        workDir = new File(baseDir + File.separator + sourceDir);

        if (null == version || 0 == version.length()) {
            version = project.getVersion();
        }

        getLog().info("Updating iOS version");
    }

    protected void updateVersion() throws IOSException {
        updateMarketingVersion();
        updateBuildNumber();

        String currentBuildNumber = getCurrentBuildNumber();
        getLog().info("Updated iOS version to " + version + " (" + currentBuildNumber + ")");
    }

    protected void updateMarketingVersion() throws IOSException {
        ProcessBuilder pb = new ProcessBuilder(
                "agvtool",
                "new-marketing-version",
                version);
        pb.directory(workDir);
        executeCommand(pb);
    }

    protected void updateBuildNumber() throws IOSException {
        if (buildNumber != null) {
            ProcessBuilder pb = new ProcessBuilder(
                    "agvtool",
                    "new-version",
                    "-all",
                    buildNumber);
            pb.directory(workDir);
            executeCommand(pb);
        } else if (incrementBuildNumber) {
            ProcessBuilder pb = new ProcessBuilder(
                    "agvtool",
                    "next-version",
                    "-all");
            pb.directory(workDir);
            executeCommand(pb);
        }
    }

    protected String getCurrentBuildNumber() throws IOSException {
        ProcessBuilder pb = new ProcessBuilder(
                "agvtool",
                "what-version",
                "-terse");
        pb.directory(workDir);

        return executeCommand(pb);
    }
}
