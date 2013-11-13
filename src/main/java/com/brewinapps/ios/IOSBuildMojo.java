package com.brewinapps.ios;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;


/**
 * @author Brewin' Apps AS
 * @goal build
 * @phase compile
 */
public class IOSBuildMojo extends IOSAbstractMojo {

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
     * If the install/update of the pods should be skipped (assuming the project uses CocoaPods)
     *
     * @parameter property="ios.skipPodsUpdate"
     *            default-value="false"
     */
    private boolean skipPodsUpdate;

    /**
     * iOS project name
     *
     * @parameter property="ios.projectName"
     */
    private String projectName;

    /**
     * iOS workspace name
     *
     * @parameter property="ios.workspaceName"
     */
    private String workspaceName;

    /**
     * iOS scheme
     *
     * @parameter property="ios.scheme"
     */
    private String scheme;

    /**
     * iOS scheme
     *
     * @parameter property="ios.target"
     */
    private String target;

    /**
     * iOS sdk
     *
     * @parameter property="ios.sdk"
     */
    private String sdk;

    /**
     * iOS build configuration
     *
     * @parameter property="ios.buildConfiguration"
     */
    private String buildConfiguration;

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

    private String baseDir;
    private File targetDir;
    private File workDir;
    private String appDir;


    /**
     *
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        initialize();

        try {
            validateParameters();
            unlockKeychain();
            build();
            renameFiles();
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

        if (null == buildConfiguration) {
            buildConfiguration = DEFAULT_BUILD_CONFIGURATION;
        }

        if (null == sdk) {
            sdk = DEFAULT_SDK;
        }

        baseDir = project.getBasedir().toString();
        targetDir = new File(project.getBuild().getDirectory());
        workDir = new File(baseDir + File.separator + sourceDir);
        appDir = targetDir + File.separator + buildConfiguration + "-" + DEFAULT_SDK + File.separator;
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
        xcrun();
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

    protected void xcrun() throws IOSException {
        List<String> parameters = createXcrunParameters();

        ProcessBuilder pb = new ProcessBuilder(parameters);
        pb.directory(workDir);
        executeCommand(pb);
    }

    protected void renameFiles() throws IOException {
        String dSYMFilePath = appDir + appName + ".app.dSYM";
        if (FileUtils.fileExists(dSYMFilePath)) {
            String dSYMNewFilePath= appDir + project.getBuild().getFinalName() + ".app.dSYM";
            File dSYMFile = new File(dSYMFilePath);
            File dSYMNewFile = new File(dSYMNewFilePath);

            getLog().debug("Renaming " + dSYMFilePath + " to " + dSYMNewFilePath);
            FileUtils.deleteDirectory(dSYMNewFile);
            FileUtils.rename(dSYMFile, dSYMNewFile);
        }
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

    protected List<String> createXcrunParameters() {
        List<String> parameters = new ArrayList<String>();
        parameters.add("xcrun");

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
