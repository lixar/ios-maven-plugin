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
     * iOS code sign identity
     *
     * @parameter property="ios.codeSignIdentity"
     */
    private String codeSignIdentity;

    /**
     *
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initialize();
            xcrun();
            packageDsym();

            project.getArtifact().setFile(new File(getArtifactPath("ipa")));
        } catch (IOSException e) {
            getLog().error(e.getMessage());
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (Exception e) {
            getLog().error(e.getMessage());
            throw new MojoFailureException(e.getMessage());
        }
    }

    protected void packageDsym() throws IOSException {
        ProcessBuilder pb = new ProcessBuilder(
                "zip",
                "-r",
                getArtifactFilename("dSYM.zip"),
                appName + ".app.dSYM");
        pb.directory(new File(appDir));
        executeCommand(pb);
    }

    protected void xcrun() throws IOSException {
        List<String> parameters = createXcrunParameters();

        ProcessBuilder pb = new ProcessBuilder(parameters);
        pb.directory(new File(appDir));
        executeCommand(pb);
    }

    protected List<String> createXcrunParameters() {
        List<String> parameters = new ArrayList<String>();
        parameters.add("xcrun");

        parameters.add("-sdk");
        parameters.add(sdk);
        parameters.add("PackageApplication");
        parameters.add("-v");
        parameters.add(appName + ".app");
        parameters.add("-o");
        parameters.add(getArtifactPath("ipa"));

        if (codeSignIdentity != null && codeSignIdentity.length() > 0) {
            parameters.add("--sign");
            parameters.add(codeSignIdentity);
        }

        return parameters;
    }
}
