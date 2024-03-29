/**
 * Copyright © 2018 Nelson Tavares de Sousa (tavaresdesousa@email.uni-kiel.de)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.store.impl;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import de.gerdiproject.store.AbstractStoreService;
import de.gerdiproject.store.datamodel.CopyStatus;
import de.gerdiproject.store.datamodel.ListElement;
import de.gerdiproject.store.datamodel.ResearchDataInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an implementation of a GeRDI store service using WebDAV as a provider.
 */
public class WebDAVStoreService extends AbstractStoreService<WebDAVCreds> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(WebDAVStoreService.class);

    public WebDAVStoreService() {
        super();
    }

    /**
     * This gets this program goin'
     *
     * @param args Command line arguments - not used here
     */
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
    protected boolean copyFile(final WebDAVCreds creds, final String targetDir, final ResearchDataInputStream inputStream) {
        final URL url;
        final String[] splitFile = inputStream.getName().split("/");
        final String fileName = splitFile[splitFile.length - 1];
        final Sardine client = creds.getClient();
        final String appendedDir = creds.getDomain().concat(targetDir);
        try {
            if (client.exists(appendedDir + fileName)) { // TODO: new file name with with a suffix
                inputStream.setStatus(CopyStatus.ERROR); // File already exists
            } else {
                new Thread(() -> {
                    try {
                        inputStream.setStatus(CopyStatus.RUNNING);
                        client.put(appendedDir + fileName, inputStream);
                    } catch (IOException e) {
                        inputStream.setStatus(CopyStatus.ERROR);
                        return;
                    }
                    inputStream.setStatus(CopyStatus.FINISHED);
                }).start();
            }
        } catch (IOException e) {
            LOGGER.error("Error while copying files.", e);
            inputStream.setStatus(CopyStatus.ERROR);
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
            return null;
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
            final String displayName = element.getDisplayName() != null ? element.getDisplayName() : element.getName(); //NOPMD ternary operator makes it easier here
            final String uriPath = prefix.equals("") ? element.getPath() : element.getPath().replace(prefix, ""); //NOPMD ternary operator makes it easier here
            ret.add(ListElement.of(displayName,
                    element.getContentType(),
                    uriPath));
        }
        return ret;
    }

}
