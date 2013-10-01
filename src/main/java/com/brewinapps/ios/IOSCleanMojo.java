package com.brewinapps.ios;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author Sylvain Guillope
 * @goal clean
 * @phase clean
 */
public class IOSCleanMojo extends IOSAbstractMojo {
	
	/**
	 * iOS Source Directory
	 * @parameter
	 * 		expression="${ios.sourceDir}"
	 * 		default-value="."
	 */
	private String sourceDir;

    /**
     * iOS build configuration
     * @parameter
     * 		expression="${ios.buildConfiguration}"
     */
    private String buildConfiguration;

	/**
	 * If the Pods folder and Podfile.lock file should be deleted during the clean
     * @parameter
	 * 		expression="${ios.cleanPods}"
	 * 		default-value="true"
	 */
	private boolean cleanPods;
	
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
	
	void initialize() {
		baseDir = project.getBasedir().toString();
		targetDir = new File(project.getBuild().getDirectory());
		workDir = new File(baseDir + File.separator + sourceDir);
	}

	void clean() throws IOSException, IOException {
        xcodebuildClean();

        if (cleanPods) {
            getLog().info("Cleaning CocoaPods files");
            cleanPods();
        }
        else {
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
            }
            else {
                getLog().warn("Failed to delete file " + podfileLockPath);
            }
        }
        else {
            getLog().debug("Skipping deletion of " + podfileLockPath);
        }
        if (podsFolder.exists()) {
            try {
                FileUtils.deleteDirectory(podsFolder);
                getLog().info("Successfully deleted directory " + podsFolderPath);
            } catch (IOException e) {
                getLog().warn("Failed to delete directory " + podsFolderPath, e);
            }
        }
        else {
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
		parameters.add("xcodebuild");
        parameters.add("clean");
        parameters.add("-alltargets");

        if (null != buildConfiguration) {
            parameters.add("-configuration");
            parameters.add(buildConfiguration);
        }

        return parameters;
	}
}
