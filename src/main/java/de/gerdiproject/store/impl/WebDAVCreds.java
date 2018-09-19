package de.gerdiproject.store.impl;

import com.github.sardine.Sardine;
import de.gerdiproject.store.datamodel.Credentials;
import lombok.Data;

public @Data class WebDAVCreds implements Credentials {

    private final Sardine client;

}
