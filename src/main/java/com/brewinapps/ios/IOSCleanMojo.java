package com.brewinapps.ios;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Sylvain Guillope
 * @goal clean
 * @phase clean
 */
public class IOSCleanMojo extends IOSAbstractMojo {
    /**
     * If the Pods folder and Podfile.lock file should be deleted during the clean
     *
     * @parameter property="ios.cleanPods"
     *            default-value="true"
     */
    private boolean cleanPods;

    /**
     *
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        initialize();

        try {
            clean();
        } catch (IOSException e) {
            getLog().error(e.getMessage());
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (Exception e) {
            getLog().error(e.getMessage());
            throw new MojoFailureException(e.getMessage());
        }
    }

    void clean() throws IOSException, IOException {
        xcodebuildClean();

        if (cleanPods) {
            getLog().info("Cleaning CocoaPods files");
            cleanPods();
        } else {
            getLog().info("Skipping cleaning of CocoaPods files");
        }
    }

    void cleanPods() {
        File podfileLock = new File(workDir + File.separator + "Podfile.lock");
        File podsFolder = new File(workDir + File.separator + "Pods");

        String podfileLockPath = podfileLock.getAbsolutePath();
        String podsFolderPath = podsFolder.getAbsolutePath();

        if (podfileLock.exists()) {
            if (podfileLock.delete()) {
                getLog().info("Successfully deleted file " + podfileLockPath);
            } else {
                getLog().warn("Failed to delete file " + podfileLockPath);
            }
        } else {
            getLog().debug("Skipping deletion of " + podfileLockPath);
        }
        if (podsFolder.exists()) {
            try {
                FileUtils.deleteDirectory(podsFolder);
                getLog().info("Successfully deleted directory " + podsFolderPath);
            } catch (IOException e) {
                getLog().warn("Failed to delete directory " + podsFolderPath, e);
            }
        } else {
            getLog().debug("Skipping deletion of " + podsFolderPath);
        }
    }

    protected void xcodebuildClean() throws IOSException {
        List<String> parameters = createXcodebuildCleanParameters();

        ProcessBuilder pb = new ProcessBuilder(parameters);
        pb.directory(workDir);
        executeCommand(pb);
    }

    protected List<String> createXcodebuildCleanParameters() {
        List<String> parameters = new ArrayList<String>();
        parameters.add(getBuildCommand());

        if (workspaceName != null) {
            String workspaceSuffix = ".xcworkspace";
            if (!workspaceName.endsWith(workspaceSuffix)) {
                workspaceName += workspaceSuffix;
            }

            parameters.add("-workspace");
            parameters.add(workspaceName);
        } else if (projectName != null) {
            String projectSuffix = ".xcodeproj";
            if (!projectName.endsWith(projectSuffix)) {
                projectName += projectSuffix;
            }

            parameters.add("-project");
            parameters.add(projectName);
        }

        if (scheme != null) {
            parameters.add("-scheme");
            parameters.add(scheme);
        } else if (null != target) {
            parameters.add("-target");
            parameters.add(target);
        }

        parameters.add("-sdk");
        parameters.add(sdk);

        if (null != buildConfiguration) {
            parameters.add("-configuration");
            parameters.add(buildConfiguration);
        }

        parameters.add("SYMROOT=" + targetDir.getAbsolutePath());
        parameters.add("SHARED_PRECOMPS_DIR=" + project.getBuild().getDirectory() + File.separator + DEFAULT_SHARED_PRECOMPS_DIR);

        parameters.add("clean");

        return parameters;
    }
}
