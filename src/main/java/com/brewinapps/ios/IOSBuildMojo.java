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
	private String appDir;
	
	
	/**
	 * 
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		intialize();
		
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
	
	protected void intialize() {
		if (null == buildParams.get("buildConfiguration")) {
			buildParams.put("buildConfiguration", DEFAULT_BUILD_CONFIGURATION);
		}
		
		baseDir = project.getBasedir().toString();
		targetDir = new File(project.getBuild().getDirectory());
		workDir = new File(baseDir + File.separator + sourceDir);
		appDir = targetDir + File.separator + buildParams.get("buildConfiguration") + "-" + DEFAULT_SDK + File.separator;
	}
	
	protected void validateParameters() throws IOSException {
		if (buildParams.get("workspace") != null && buildParams.get("scheme") == null) {
			throw new IOSException("The 'scheme' parameter is required when building a workspace");
		}
		
		if (!workDir.exists()) {
			throw new IOSException("Invalid sourceDir specified: " + workDir.getAbsolutePath());
		}
	}
	
	protected void build() throws IOSException {
		
		xcodebuild();
		xcrun();
	}
	
		}
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
		
		if (buildParams.get("settings") != null) {
			parameters.add(buildParams.get("settings"));
		}
		
		return parameters;
	}
	
	protected List<String> createXcrunParameters() {
		List<String> parameters = new ArrayList<String>();
		parameters.add("xcrun");
		
		parameters.add("-sdk");
		parameters.add(DEFAULT_SDK);
		parameters.add("PackageApplication");
		parameters.add("-v");
		parameters.add(appDir + appName + ".app");
		parameters.add("-o");
		parameters.add(appDir + appName + ".ipa");
		
		if (buildParams.get("codeSignIdentity") != null) {
			parameters.add("--sign");
			parameters.add(buildParams.get("codeSignIdentity"));
		}
		
		return parameters;
	}
}
