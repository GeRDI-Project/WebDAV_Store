package de.gerdiproject.store.impl;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import de.gerdiproject.store.AbstractStoreService;
import de.gerdiproject.store.datamodel.ListElement;
import de.gerdiproject.store.datamodel.ResearchDataInputStream;
import de.gerdiproject.store.datamodel.TaskElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class WebDAVStoreService extends AbstractStoreService<WebDAVCreds> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(WebDAVStoreService.class);

    public WebDAVStoreService(){
        super(null);
    }

    public static void main(final String[] args) {
        final WebDAVStoreService service = new WebDAVStoreService();
        service.registerStaticFolder("/static");
        service.run();
    }

    @Override
    protected boolean isLoggedIn(WebDAVCreds credentials) {
        return credentials != null;
    }

    @Override
    protected WebDAVCreds login(Request request, Response response) {
        String usr = request.queryParams("username");
        String domain = request.queryParams("webdavDomain");
        String pw = request.queryParams("password");
        Sardine client = SardineFactory.begin(usr, pw);
        try {
            client.list(domain);
        } catch (Exception e) {
            LOGGER.error("Could not connect to WebDAV server.", e);
            response.redirect("/api/v1/store/login/init.html");
            return null;
        }
        response.redirect("/api/v1/store/login/close.html");
        return new WebDAVCreds(client, domain);
    }


    @Override
    protected boolean copyFile(WebDAVCreds creds, String targetDir, TaskElement taskElement) {
        final Sardine client = creds.getClient();
        final String appendedDir = creds.getDomain().concat(targetDir);
        final URL url;
        try {
            url = new URL(taskElement.getFileName());
        } catch (MalformedURLException e) {
            LOGGER.error("Encountered malformed URL.", e);
            return false;
        }
        String[] splitFile = url.getFile().split("/");
        String fileName = splitFile[splitFile.length-1];
        try {
            if (!client.exists( appendedDir + fileName)) {
                client.put(appendedDir + fileName, new ResearchDataInputStream(url, taskElement));
                taskElement.setProgressInPercent(100);
            }
        } catch (IOException e) {
            LOGGER.error("Error while copying files.", e);
            taskElement.setProgressInPercent(-2);
            return false;
        }
        return true;
    }

    @Override
    protected List<ListElement> listFiles(String directory, WebDAVCreds creds) {
        final String appendedDir = creds.getDomain().concat(directory);
        final Sardine client = creds.getClient();
        client.enablePreemptiveAuthentication(creds.getDomain());
        List<DavResource> elems = null;
        try {
            elems = client.list(appendedDir);
        } catch (IOException e) {
            LOGGER.error("Error while retrieving file list.", e);
        }
        List<ListElement> ret = new ArrayList<>();

        final URI uri;
        String prefix = "";
        try {
            uri = new URI(creds.getDomain());
            prefix = uri.getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        for(int i = 1; i < elems.size(); i++) { // Skip first... this is the requested directory itself
            final DavResource it = elems.get(i);
            final String displayName = it.getDisplayName() != null ? it.getDisplayName() : it.getName();
            ret.add(ListElement.of(displayName,
                    it.getContentType(),
                    it.getPath().replace(prefix, "")));
        }
        return ret;
    }

}
