package de.gerdiproject.store.impl;

import com.github.sardine.Sardine;
import de.gerdiproject.store.datamodel.ICredentials;
import lombok.Data;

public @Data class WebDAVCreds implements ICredentials {

    private final Sardine client;
    private final String domain;

}
