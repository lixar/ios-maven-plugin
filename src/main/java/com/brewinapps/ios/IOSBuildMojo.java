package com.brewinapps.ios;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;


/**
 * 
 * @author Brewin' Apps AS
 * @goal build
 * @phase compile
 */
public class IOSBuildMojo extends IOSAbstractMojo {
	
	/**
	 * iOS Source Directory
	 * @parameter
	 * 		expression="${ios.sourceDir}"
	 * 		default-value="."
	 */
	private String sourceDir;
	
	/**
	 * iOS app name
	 * @parameter
	 * 		expression="${ios.appName}"
	 * @required
	 */
	private String appName;
	
	/**
	 * iOS build parameters
	 * @parameter
	 */
	private Map<String, String> buildParams;
	
	/**
	 * Keychain parameters
	 * @parameter
	 */
	private Map<String, String> keychainParams;

	/**
	 * iOS version 
	 * @parameter
	 * 		expression="${ios.version}"
	 * 		default-value="${project.version}"
	 */
	private String version;
	
	/**
	 * build id
	 * @parameter
	 * 		expression="${ios.buildId}"
	 */
	private String buildId;

	/**
	 * If the build number should be incremented
	 * @parameter
	 * 		expression="${ios.incrementBuildNumber}" 
	 * 		default-value=false
	 */
	private boolean incrementBuildNumber;
	
	/**
	* The maven project.
	* 
	* @parameter expression="${project}"
	* @required
	* @readonly
	*/
	protected MavenProject project;
	
	private String baseDir;
	private File targetDir;
	private File workDir;
	
	
	/**
	 * 
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		intialize();
		
		try {
			validateParameters();
			unlockKeychain();
			build();
		}
		catch (IOSException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		catch (Exception e) {
			getLog().error(e.getMessage());
			throw new MojoFailureException(e.getMessage());
		}
	}
	
	protected void intialize() {
		baseDir = project.getBasedir().toString();
		targetDir = new File(project.getBuild().getDirectory());
		workDir = new File(baseDir + "/" + sourceDir);
	}
	
	protected void validateParameters() throws IOSException {
		if (buildParams.get("workspace") != null && buildParams.get("scheme") == null) {
			throw new IOSException("The 'scheme' parameter is required when building a workspace");
		}
		
		if (!workDir.exists()) {
			throw new IOSException("Invalid sourceDir specified: " + workDir.getAbsolutePath());
		}
		
		if (null == buildParams.get("buildConfiguration")) {
			buildParams.put("buildConfiguration", DEFAULT_BUILD_CONFIGURATION);
		}
	}
	
	protected void build() throws IOSException {
		updateMarketingVersion();
		updateBuildNumber();
		
		xcodebuild();
		xcrun();
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
		if (buildId != null) {
			ProcessBuilder pb = new ProcessBuilder(
					"agvtool",
					"new-version",
					"-all",
					buildId);
			pb.directory(workDir);
			executeCommand(pb);
		}
		else if (incrementBuildNumber) {
			ProcessBuilder pb = new ProcessBuilder(
					"agvtool",
					"next-version",
					"-all");
			pb.directory(workDir);
			executeCommand(pb);
		}
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
	
	protected void xcrun()  throws IOSException {
		List<String> parameters = createXcrunParameters();
		
		ProcessBuilder pb = new ProcessBuilder(parameters);
		pb.directory(workDir);
		executeCommand(pb);
	}
	
	protected List<String> createXcodebuildParameters() {
		List<String> parameters = new ArrayList<String>();
		parameters.add("xcodebuild");
		
		if (buildParams.get("workspace") != null) {
			String workspaceName = buildParams.get("workspace");
			String workspaceSuffix = ".xcworkspace";
			if (!workspaceName.endsWith(workspaceSuffix)) {
				workspaceName += workspaceSuffix;
			}
			
			parameters.add("-workspace");
			parameters.add(workspaceName);
		}
		else if (buildParams.get("project") != null) {
			String projectName = buildParams.get("project");
			String projectSuffix = ".xcodeproj";
			if (!projectName.endsWith(projectSuffix)) {
				projectName += projectSuffix;
			}
			
			parameters.add("-project");
			parameters.add(projectName);
		}
		
		if (buildParams.get("scheme") != null) {
			parameters.add("-scheme");
			parameters.add(buildParams.get("scheme"));
		}
		
		parameters.add("-sdk");
		parameters.add(DEFAULT_SDK);
		parameters.add("-configuration");
		parameters.add(buildParams.get("buildConfiguration"));
		
		parameters.add("SYMROOT=" + targetDir.getAbsolutePath());
		
		if (buildParams.get("codeSignIdentity") != null) {
			parameters.add("CODE_SIGN_IDENTITY=" + buildParams.get("codeSignIdentity"));
		}
		
		return parameters;
	}
	
	protected List<String> createXcrunParameters() {
		String artifactsPath = targetDir + "/" + buildParams.get("buildConfiguration") + "-" + DEFAULT_SDK + "/";
		
		List<String> parameters = new ArrayList<String>();
		parameters.add("xcrun");
		
		parameters.add("-sdk");
		parameters.add(DEFAULT_SDK);
		parameters.add("PackageApplication");
		parameters.add("-v");
		parameters.add(artifactsPath + appName + ".app");
		parameters.add("-o");
		parameters.add(artifactsPath + appName + ".ipa");
		
		if (buildParams.get("codeSignIdentity") != null) {
			parameters.add("--sign");
			parameters.add(buildParams.get("codeSignIdentity"));
		}
		
		return parameters;
	}
}
