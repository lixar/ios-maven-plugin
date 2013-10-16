package com.brewinapps.ios;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


/**
 * @author Brewin' Apps AS
 * @goal deploy
 * @phase deploy
 */
public class IOSDeployMojo extends IOSAbstractMojo {

    /**
     * HockeyApp Configuration
     *
     * @parameter expression="${ios.hockeyApp}"
     */
    private Map<String, String> hockeyApp;

    /**
     * iOS App name
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

    private String targetDir;
    private String appDir;

    static final String HOCKEYAPP_UPLOAD_URL = "https://rink.hockeyapp.net/api/2/apps/%s/app_versions";

    /**
     *
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initialize();
            validateParameters();

            deploy();
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

    protected void deploy() throws IOSException {
        getLog().info("Deploying to HockeyApp...");

        try {
            prepareDSYMPackage();
            uploadToHockeyApp();

        } catch (Exception e) {
            throw new IOSException("An error occured while deploying build to HockeyApp: " + e.getMessage());
        }
    }

    protected void validateParameters() throws IOSException {
        if (hockeyApp.get("apiToken") == null) {
            throw new IOSException("The 'hockeyAppToken' parameter is required to upload to Hockey App");
        }
        if (hockeyApp.get("appIdentifier") == null) {
            throw new IOSException("The 'hockeyAppIdentifier' parameter is required to upload to Hockey App");
        }

        if (null == buildConfiguration) {
            buildConfiguration = DEFAULT_BUILD_CONFIGURATION;
        }

        String ipaPath = appDir + project.getBuild().getFinalName() + ".ipa";
        if (!(new File(ipaPath)).exists()) {
            throw new IOSException("Could not find ipa file at '" + ipaPath + "'. You must compile the artifact before deploying.");
        }
    }

    protected void prepareDSYMPackage() throws IOSException {
        ProcessBuilder pb = new ProcessBuilder(
                "zip",
                "-r",
                appName + ".dSYM.zip",
                appName + ".app.dSYM");
        pb.directory(new File(appDir));
        executeCommand(pb);
    }

    protected void uploadToHockeyApp() throws IOSException {
        HttpClient client = createHttpClient();
        HttpPost post = createHockeyAppHttpPost();
        MultipartEntity entity = createHockeyAppMultipartEntity();

        post.setEntity(entity);

        // Run the request
        HttpResponse response;
        try {
            response = client.execute(post);
        } catch (ClientProtocolException e) {
            throw new IOSException(e);
        } catch (IOException e) {
            throw new IOSException(e);
        }

        HttpEntity responseEntity = response.getEntity();

        getLog().info(response.getStatusLine().toString());
        if (responseEntity != null) {
            try {
                getLog().info(EntityUtils.toString(responseEntity));
            } catch (ParseException e) {
                throw new IOSException(e);
            } catch (IOException e) {
                throw new IOSException(e);
            }
        }

        client.getConnectionManager().shutdown();
    }

    protected HttpClient createHttpClient() {
        HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        return client;
    }

    protected HttpPost createHockeyAppHttpPost() {
        String hockeyAppUrl = String.format(HOCKEYAPP_UPLOAD_URL, hockeyApp.get("appIdentifier"));
        HttpPost post = new HttpPost(hockeyAppUrl);
        post.addHeader("X-HockeyAppToken", hockeyApp.get("apiToken"));

        return post;
    }

    protected MultipartEntity createHockeyAppMultipartEntity() throws IOSException {
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

        File ipaFile = new File(appDir + project.getBuild().getFinalName() + ".ipa");
        File dsymZipFile = new File(appDir + appName + ".dSYM.zip");

        entity.addPart("ipa", new FileBody(ipaFile, "application/zip"));
        entity.addPart("dsym", new FileBody(dsymZipFile, "application/zip"));

        StringBody notesBody = createStringBody("notes");
        if (null != notesBody) {
            entity.addPart("notes", notesBody);
        }

        StringBody notesTypeBody = createStringBody("notesType");
        if (null != notesTypeBody) {
            entity.addPart("notes_type", notesTypeBody);
        }

        StringBody notifyBody = createStringBody("notify");
        if (null != notifyBody) {
            entity.addPart("notify", notifyBody);
        }

        StringBody statusBody = createStringBody("status");
        if (null != statusBody) {
            entity.addPart("status", statusBody);
        }

        StringBody mandatoryBody = createStringBody("mandatory");
        if (null != mandatoryBody) {
            entity.addPart("mandatory", mandatoryBody);
        }

        StringBody tagsBody = createStringBody("tags");
        if (null != tagsBody) {
            entity.addPart("tags", tagsBody);
        }

        return entity;
    }

    protected StringBody createStringBody(String paramName) throws IOSException {
        StringBody body = null;
        String paramValue = hockeyApp.get(paramName);
        if (paramValue != null) {
            try {
                body = new StringBody(paramValue, "text/plain", Charset.forName("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IOSException(e);
            }
        }

        return body;
    }
}
