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

import com.github.sardine.Sardine;
import de.gerdiproject.store.datamodel.ICredentials;
import lombok.Data;

/**
 * This class represents the required credentials to use WebDAV as a provider.
 */
public @Data class WebDAVCreds implements ICredentials {

    /**
     * The sardine client instance, which was already created with the user credentials.
     */
    private final Sardine client;
    /**
     * The target domain if the WebDAV provider.
     */
    private final String domain;

}
