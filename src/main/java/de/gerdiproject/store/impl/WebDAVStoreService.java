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

    public WebDAVStoreService() {
        super(null);
    }

    public static void main(final String[] args) {
        final WebDAVStoreService service = new WebDAVStoreService();
        service.registerStaticFolder("/static");
        service.run();
    }

    @Override
    protected boolean isLoggedIn(final WebDAVCreds credentials) {
        return credentials != null;
    }

    @Override
    protected WebDAVCreds login(final Request request, final Response response) {
        final String usr = request.queryParams("username");
        final String domain = request.queryParams("webdavDomain");
        final String password = request.queryParams("password");
        final Sardine client = SardineFactory.begin(usr, password);
        try {
            client.list(domain);
        } catch (IOException e) {
            LOGGER.error("Could not connect to WebDAV server.", e);
            response.redirect("/api/v1/store/login/init.html");
            return null;
        }
        response.redirect("/api/v1/store/login/close.html");
        return new WebDAVCreds(client, domain);
    }


    @Override
    protected boolean copyFile(final WebDAVCreds creds, final String targetDir, final TaskElement taskElement) {
        final URL url;
        try {
            url = new URL(taskElement.getFileName());
        } catch (MalformedURLException e) {
            LOGGER.error("Encountered malformed URL.", e);
            return false;
        }
        final String[] splitFile = url.getFile().split("/");
        final String fileName = splitFile[splitFile.length - 1];
        final Sardine client = creds.getClient();
        final String appendedDir = creds.getDomain().concat(targetDir);
        try {
            if (client.exists(appendedDir + fileName)) { // TODO: new file name with with a suffix
                taskElement.setProgressInPercent(-2); // File already exists
            } else {
                new Thread(() -> {
                    try {
                        client.put(appendedDir + fileName, new ResearchDataInputStream(url, taskElement));
                    } catch (IOException e) {
                        taskElement.setProgressInPercent(-2);
                        return;
                    }
                    taskElement.setProgressInPercent(100);
                }).start();
            }
        } catch (IOException e) {
            LOGGER.error("Error while copying files.", e);
            taskElement.setProgressInPercent(-2);
            return false;
        }
        return true;
    }

    @Override
    protected List<ListElement> listFiles(final String directory, final WebDAVCreds creds) {
        final String appendedDir = creds.getDomain().concat(directory);
        final Sardine client = creds.getClient();
        client.enablePreemptiveAuthentication(creds.getDomain());
        List<DavResource> elems = null;
        try {
            elems = client.list(appendedDir);
        } catch (IOException e) {
            LOGGER.error("Error while retrieving file list.", e);
        }
        final List<ListElement> ret = new ArrayList<>();

        final URI uri;
        String prefix = "";
        try {
            uri = new URI(creds.getDomain());
            prefix = uri.getPath();
        } catch (URISyntaxException e) {
            LOGGER.error("Error parsing path.", e);
        }

        for (int i = 1; i < elems.size(); i++) { // Skip first... this is the requested directory itself
            final DavResource element = elems.get(i);
            final String displayName = element.getDisplayName() != null ? element.getDisplayName() : element.getName(); //NOPMD
            ret.add(ListElement.of(displayName,
                    element.getContentType(),
                    element.getPath().replace(prefix, "")));
        }
        return ret;
    }

}
