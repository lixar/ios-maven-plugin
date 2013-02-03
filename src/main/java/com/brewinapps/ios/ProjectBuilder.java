package com.brewinapps.ios;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Brewin' Apps AS
 */
public class ProjectBuilder {
	
	/**
	 * @param properties
	 * @throws IOSException
	 */
	public static void build(final Map<String, String> properties) throws IOSException {
		
		validateProperties(properties);
		
		// Make sure the source directory exists
		File workDir = new File(properties.get("baseDir") + "/" + properties.get("sourceDir"));
		if (!workDir.exists()) {
			throw new IOSException("Invalid sourceDir specified: " + workDir.getAbsolutePath());
		}
		
		File targetDir = new File(properties.get("targetDir"));
		
		// Run agvtool to stamp marketing version
		ProcessBuilder pb = new ProcessBuilder(
				"agvtool",
				"new-marketing-version",
				properties.get("version"));
		pb.directory(workDir);
		CommandHelper.performCommand(pb);
		
		// Run agvtool to stamp build if a build id is specified
		if (properties.get("buildId") != null) {
			pb = new ProcessBuilder(
					"agvtool",
					"new-version",
					"-all",
					properties.get("buildId"));
			pb.directory(workDir);
			CommandHelper.performCommand(pb);
		}
		
		// Build the application
		List<String> buildParameters = new ArrayList<String>();
		buildParameters.add("xcodebuild");
		
		if (properties.get("workspaceName") != null) {
			String workspaceName = properties.get("workspaceName");
			String workspaceSuffix = ".xcworkspace";
			if (!workspaceName.endsWith(workspaceSuffix)) {
				workspaceName += workspaceSuffix;
			}
			
			buildParameters.add("-workspace");
			buildParameters.add(workspaceName);
		}
		else if (properties.get("projectName") != null) {
			String projectName = properties.get("projectName");
			String projectSuffix = ".xcodeproj";
			if (!projectName.endsWith(projectSuffix)) {
				projectName += projectSuffix;
			}
			
			buildParameters.add("-project");
			buildParameters.add(projectName);
		}
		
		if (properties.get("scheme") != null) {
			buildParameters.add("-scheme");
			buildParameters.add(properties.get("scheme"));
		}
		
		buildParameters.add("-sdk");
		buildParameters.add(properties.get("sdk"));
		buildParameters.add("-configuration");
		buildParameters.add(properties.get("configuration"));
		buildParameters.add("SYMROOT=" + targetDir.getAbsolutePath());
		buildParameters.add("CODE_SIGN_IDENTITY=" + properties.get("codeSignIdentity"));
		
		pb = new ProcessBuilder(buildParameters);
		pb.directory(workDir);
		CommandHelper.performCommand(pb);
		
		// Generate IPA
		pb = new ProcessBuilder(
				"xcrun",
				"-sdk", "iphoneos",
				"PackageApplication",
				"-v", targetDir + "/" + properties.get("configuration") + "-iphoneos/" + properties.get("appName") + ".app",
				"-o", targetDir + "/" + properties.get("configuration") + "-iphoneos/" + properties.get("appName") + ".ipa",
				"--sign", properties.get("codeSignIdentity"));
		pb.directory(workDir);
		CommandHelper.performCommand(pb);
	}
	
	protected static void validateProperties(final Map<String, String> properties) throws IOSException {
		if (properties.get("workspaceName") != null && properties.get("scheme") == null) {
			throw new IOSException("The 'scheme' parameter is required when building a workspace");
		}
	}
}
