package de.gerdiproject.store.impl;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import de.gerdiproject.store.AbstractStoreService;
import de.gerdiproject.store.datamodel.Credentials;
import de.gerdiproject.store.datamodel.ListElement;
import de.gerdiproject.store.datamodel.TaskElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.net.MalformedURLException;
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
        String pw = request.queryParams("password");
        Sardine client = SardineFactory.begin(usr, pw);
        try {
            client.list("https://syncandshare.lrz.de/webdav/");
        } catch (Exception e) {
            response.redirect("/api/v1/store/login/init.html");
            return null;
        }
        response.redirect("/api/v1/store/login/close.html");
        return new WebDAVCreds(client);
    }


    @Override
    protected boolean copyFile(WebDAVCreds creds, String targetDir, TaskElement taskElement) {
        final Sardine client = creds.getClient();
        final String appendedDir = "/webdav".concat(targetDir);
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
            if (!client.exists("https://syncandshare.lrz.de" + appendedDir + fileName)) {
                client.put("https://syncandshare.lrz.de" + appendedDir + fileName, new ResearchDataInputStream(url, taskElement));
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
        final String appendedDir = "/webdav".concat(directory);
        final Sardine client = creds.getClient();
        client.enablePreemptiveAuthentication("https://syncandshare.lrz.de/");
        List<DavResource> elems = null;
        try {
            elems = client.list("https://syncandshare.lrz.de" + appendedDir);
        } catch (IOException e) {
            LOGGER.error("Error while retrieving file list.", e);
        }
        List<ListElement> ret = new ArrayList<>();
        for(int i = 1; i < elems.size(); i++) { // Skip first... this is the directory itself
            DavResource it = elems.get(i);
            ret.add(ListElement.of(it.getDisplayName(),
                    it.getContentType(),
                    it.getPath().replace("/webdav", "")));
        }
        return ret;
    }

//        post("/login/:sessionId", (request, response) -> {
//            String usr = request.queryParams("username");
//            String pw = request.queryParams("password");
//            Sardine client = SardineFactory.begin(usr, pw);
//            try {
//                val stuff = client.list("https://syncandshare.lrz.de/webdav/");
//            } catch (Exception e) {
//                response.redirect("/api/v1/store/login/init.html");
//                return "";
//            }
//            try {
//                CACHE_MAP.get(request.params("sessionId")).setCredentials(new BasicAuthCredentials(usr,pw));
//            } catch (NullPointerException e) {
//                e.printStackTrace();
//                response.status(404);
//                return "Session does not exist.";
//            }
//            response.redirect("/api/v1/store/login/close.html");
//            return "";
//        });
//
//        get("/copy/:sessionId", (request, response) -> {
//            final String dest = "/webdav" + request.queryParamOrDefault("dir", "/");
//            String session = request.params("sessionId");
//            Task task = CACHE_MAP.get(session).getTask();
//            final BasicAuthCredentials creds = (BasicAuthCredentials) CACHE_MAP.get(session).getCredentials();
//            final Sardine client = SardineFactory.begin(creds.getUsername(), creds.getPassword());
//            client.enablePreemptiveAuthentication("https://syncandshare.lrz.de/webdav/");
//            //client.list("https://syncandshare.lrz.de/webdav/"); // If this is not called before the put, it will raise an exception
//            for (Map.Entry<String, Integer> it : CACHE_MAP.get(session).getProgress().getProgressMap().entrySet()) {
//                new Thread(){
//                    @Override
//                    public void run() {
//                        final URL url;
//                        try {
//                            url = new URL(it.getKey());
//                            String[] splitFile = url.getFile().split("/");
//                            String fileName = splitFile[splitFile.length-1];
//                            if (!client.exists("https://syncandshare.lrz.de" + dest + fileName)) {
//                                client.put("https://syncandshare.lrz.de" + dest + fileName, new ResearchDataInputStream(url, it));
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                        it.setValue(new Integer(100));
//                    }
//                }.run();
//            }
//            return "";
//        });
//
//        get("/files/:sessionId", (request, response) -> {
//            String dir = "/webdav" + request.queryParamOrDefault("dir", "/");
//            BasicAuthCredentials creds = (BasicAuthCredentials) CACHE_MAP.get(request.params("sessionId")).getCredentials();
//            if(creds == null) {
//                response.status(403);
//                return "Not logged in";
//            }
//            final Sardine client = SardineFactory.begin(creds.getUsername(), creds.getPassword());
//            client.enablePreemptiveAuthentication("https://syncandshare.lrz.de/");
//            List<DavResource> elems = client.list("https://syncandshare.lrz.de" + dir);
//            List<File> ret = new ArrayList<>();
//            for(int i = 1; i < elems.size(); i++) {
//                DavResource it = elems.get(i);
//                val elem = new File();
//                elem.setDisplayName(it.getDisplayName());
//                elem.setType(it.getContentType());
//                elem.setUri(it.getPath().replace("/webdav", ""));
//                ret.add(elem);
//            }
//            return new Gson().toJson(ret);
//        });
//    }

}
