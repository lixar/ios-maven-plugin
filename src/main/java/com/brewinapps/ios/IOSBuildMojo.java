package com.brewinapps.ios;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


/**
 * @author Brewin' Apps AS
 * @goal build
 * @phase compile
 */
public class IOSBuildMojo extends IOSAbstractMojo {

    /**
     * If the install/update of the pods should be skipped (assuming the project uses CocoaPods)
     *
     * @parameter property="ios.skipPodsUpdate"
     *            default-value="false"
     */
    private boolean skipPodsUpdate;

    /**
     * iOS code sign identity
     *
     * @parameter property="ios.codeSignIdentity"
     */
    private String codeSignIdentity;

    /**
     * iOS build settings
     *
     * @parameter
     */
    private Map<String, String> buildSettings;

    /**
     * Keychain parameters
     *
     * @parameter
     */
    private Map<String, String> keychainParams;


    /**
     *
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        initialize();

        try {
            validateParameters();
            unlockKeychain();
            build();
        } catch (IOSException e) {
            getLog().error(e.getMessage());
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (Exception e) {
            getLog().error(e.getMessage());
            throw new MojoFailureException(e.getMessage());
        }
    }

    boolean hasPodfile() {
        File podfile = new File(workDir + File.separator + "Podfile");
        getLog().info(podfile.toString());
        return podfile.exists();
    }

    protected boolean hasPodfileLock() {
        File podfileLock = new File(workDir + File.separator + "Podfile.lock");
        return podfileLock.exists();
    }

    protected void validateParameters() throws IOSException {
        if (workspaceName != null && scheme == null) {
            throw new IOSException("The 'scheme' parameter is required when building a workspace");
        }

        if (!workDir.exists()) {
            throw new IOSException("Invalid sourceDir specified: " + workDir.getAbsolutePath());
        }
    }

    protected void build() throws IOSException {
        if (!skipPodsUpdate && hasPodfile()) {
            updatePods();
        }

        xcodebuild();
    }

    protected void updatePods() throws IOSException {
        List<String> podParams = new ArrayList<String>();
        podParams.add("pod");

        if (hasPodfileLock()) {
            podParams.add("update");
        } else {
            podParams.add("install");
        }

        ProcessBuilder pb = new ProcessBuilder(podParams);
        pb.directory(workDir);
        executeCommand(pb);
    }

    protected void unlockKeychain() throws IOSException {
        if (null == keychainParams
                || null == keychainParams.get("path")
                || null == keychainParams.get("password")) {
            return;
        }

        List<String> keychainParameters = new ArrayList<String>();
        keychainParameters.add("security");
        keychainParameters.add("unlock-keychain");
        keychainParameters.add("-p");
        keychainParameters.add(keychainParams.get("password"));
        keychainParameters.add(keychainParams.get("path"));

        ProcessBuilder pb = new ProcessBuilder(keychainParameters);
        executeCommand(pb);
    }

    protected void xcodebuild() throws IOSException {
        List<String> parameters = createXcodebuildParameters();

        ProcessBuilder pb = new ProcessBuilder(parameters);
        pb.directory(workDir);
        executeCommand(pb);
    }

    protected List<String> createXcodebuildParameters() {
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

        parameters.add("-configuration");
        parameters.add(buildConfiguration);

        if (null != buildSettings) {
            for (Map.Entry<String, String> entry : buildSettings.entrySet()) {
                parameters.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        if (codeSignIdentity != null && codeSignIdentity.length() > 0) {
            parameters.add("CODE_SIGN_IDENTITY=" + codeSignIdentity);
        }
        parameters.add("SYMROOT=" + targetDir.getAbsolutePath());
        parameters.add("SHARED_PRECOMPS_DIR=" + project.getBuild().getDirectory() + File.separator + DEFAULT_SHARED_PRECOMPS_DIR);

        return parameters;
    }
}
