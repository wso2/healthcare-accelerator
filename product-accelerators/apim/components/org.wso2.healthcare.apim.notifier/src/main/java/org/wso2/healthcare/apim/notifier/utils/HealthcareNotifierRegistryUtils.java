/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.apim.notifier.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;

import java.util.Properties;

import static org.wso2.healthcare.apim.notifier.Constants.TEXT_MEDIA_TYPE;

/**
 * This class contains methods to do Registry related transactions which are particular to Healthcare operations
 */
public class HealthcareNotifierRegistryUtils {

    private static final Log LOG = LogFactory.getLog(HealthcareNotifierRegistryUtils.class);

    /**
     * This method will persist content to the provided path in the healthcare registry
     *
     * @param tenantId  ID of the logged-in user
     * @param path      Registry location of the resource to be stored
     * @param content   Content of the resource to be stored
     * @param mediaType Media type of the resource
     * @throws RegistryException Base Class for capturing any type of exception that occurs when using the Registry APIs.
     */
    public static void storeResourcesInRegistry(int tenantId, String path, String content, String mediaType)
            throws RegistryException {
        UserRegistry registry = getTheRegistryInstance(tenantId);

        ResourceImpl resource = new ResourceImpl();

        if (!StringUtils.isEmpty(content)) {
            resource.setContent(content);
            mediaType = mediaType == null ? TEXT_MEDIA_TYPE : mediaType;
            resource.setMediaType(mediaType);

            registry.beginTransaction();
            registry.put(path, resource);
            registry.commitTransaction();
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("The provided content is empty, so skip the storing process to the registry");
            }
        }
    }


    /**
     * This method will remove content from the provided path in the healthcare registry
     *
     * @param tenantId ID of the logged-in user
     * @param path     Registry location of the resource to be removed
     * @throws RegistryException Base Class for capturing any type of exception that occurs when using the Registry APIs.
     */
    public static void removeResourcesFromRegistry(int tenantId, String path) throws RegistryException {
        UserRegistry registry = getTheRegistryInstance(tenantId);

        if (registry.resourceExists(path)) {
            registry.beginTransaction();
            registry.delete(path);
            registry.commitTransaction();
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("There is no any content in the provided path" + path + ", so can't remove the content from the registry");
            }
        }
    }


    /**
     * This method will retrieve content (as String) of the provided path in the healthcare registry
     *
     * @param tenantId ID of the logged-in user
     * @param path     Registry location of the resource to be retrieved
     * @return String Registry resources stored in the provided path, and will return as string
     * @throws RegistryException Base Class for capturing any type of exception that occurs when using the Registry APIs.
     */
    public static String getResourceAsStringFromRegistry(int tenantId, String path) throws RegistryException {
        UserRegistry registry = getTheRegistryInstance(tenantId);

        Resource resource = null;
        if (registry.resourceExists(path)) {
            resource = registry.get(path);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("There is no any content in the provided path" + path + ", so can't retrieve the content from the registry");
            }
        }

        return resource != null ? RegistryUtils.decodeBytes((byte[]) resource.getContent()) : null;
    }


    /**
     * This method will retrieve content (as Registry Resource object) of the provided path in the healthcare registry
     *
     * @param tenantId ID of the logged-in user
     * @param path     Registry location of the resource to be retrieved
     * @return Resource Registry resources stored in the provided path
     * @throws RegistryException Base Class for capturing any type of exception that occurs when using the Registry APIs.
     */
    public static Resource getResourcesFromRegistry(int tenantId, String path) throws RegistryException {
        UserRegistry registry = getTheRegistryInstance(tenantId);

        Resource resource = null;
        if (registry.resourceExists(path)) {
            resource = registry.get(path);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("There is no any content in the provided path:" + path + ", so can't retrieve the content from the registry");
            }
        }

        return resource;
    }


    /**
     * This method will check whether the specific resource exist in the provided path in the healthcare registry
     *
     * @param tenantId ID of the logged-in user
     * @param path     Registry location of the resource
     * @return boolean True, If a resource found in the provided registry path
     * @throws RegistryException Base Class for capturing any type of exception that occurs when using the Registry APIs.
     */
    public static boolean isResourceExist(int tenantId, String path) throws RegistryException {
        UserRegistry registry = getTheRegistryInstance(tenantId);
        return registry.resourceExists(path);
    }


    /**
     * This method will add properties to artifact of the provided path in the healthcare registry
     *
     * @param tenantId ID of the logged-in user
     * @param path     Registry location of the resource
     * @param key      Unique identifier of the property to be added for this resource
     * @param value    Value of the property to be added for this resource
     * @throws RegistryException Base Class for capturing any type of exception that occurs when using the Registry APIs.
     */
    public static void addPropertyToResource(int tenantId, String path, String key, String value) throws RegistryException {
        UserRegistry registry = getTheRegistryInstance(tenantId);

        Resource resource = getResourcesFromRegistry(tenantId, path);
        if (resource == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("The provided content is empty, so skip the storing process to the registry");
            }
            return;
        }

        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(value)) {
            resource.addProperty(key, value);
            registry.beginTransaction();
            registry.put(path, resource);
            registry.commitTransaction();
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("The provided content is empty, so skip the storing process to the registry");
            }
        }
    }


    /**
     * This method will retrieve properties from artifact of the provided path in the healthcare registry
     *
     * @param tenantId ID of the logged-in user
     * @param path     Registry location of the resource
     * @param key      Unique identifier of the property to be added for this resource
     * @return String Value of the property stored for the provided key for this resource
     * @throws RegistryException Base Class for capturing any type of exception that occurs when using the Registry APIs.
     */
    public static String retrievePropertyOfResource(int tenantId, String path, String key) throws RegistryException {

        Resource resource = getResourcesFromRegistry(tenantId, path);
        if (resource == null || !resource.getProperties().containsKey(key)) {
            return null;
        }

        return resource.getProperty(key);
    }


    /**
     * This method will retrieve all properties from artifact of the provided path in the healthcare registry
     *
     * @param tenantId ID of the logged-in user
     * @param path     Registry location of the resource
     * @return Properties List of properties of the Registry resources stored in the provided path
     * @throws RegistryException Base Class for capturing any type of exception that occurs when using the Registry APIs.
     */
    public static Properties retrieveAllPropertiesOfResource(int tenantId, String path) throws RegistryException {

        Resource resource = getResourcesFromRegistry(tenantId, path);
        if (resource == null) {
            return null;
        }

        return resource.getProperties();
    }

    /**
     * This method will remove properties from artifact of the provided path in the healthcare registry
     *
     * @param tenantId ID of the logged-in user
     * @param path     Registry location of the resource
     * @param key      Unique identifier of the property to be added for this resource
     * @throws RegistryException Base Class for capturing any type of exception that occurs when using the Registry APIs.
     */
    public static void removePropertyFromResource(int tenantId, String path, String key) throws RegistryException {
        UserRegistry registry = getTheRegistryInstance(tenantId);

        Resource resource = getResourcesFromRegistry(tenantId, path);
        if (resource == null) {
            return;
        }

        if (resource.getProperties().containsKey(key)) {
            resource.removeProperty(key);

            registry.beginTransaction();
            registry.put(path, resource);
            registry.commitTransaction();
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("There is no any content in for the key: " + key + " in the provided path:" + path +
                        ", so can't remove the content from the registry");
            }
        }
    }

    /**
     * This method will check whether the specific property exist in the artifact of the provided path in the healthcare registry
     *
     * @param tenantId ID of the logged-in user
     * @param path     Registry location of the resource
     * @param key      Unique identifier of the property to be added for this resource
     * @return boolean True, If a property found for the provided registry path and the key
     * @throws RegistryException Base Class for capturing any type of exception that occurs when using the Registry APIs.
     */
    public static boolean isPropertyExistInResource(int tenantId, String path, String key) throws RegistryException {

        Resource resource = getResourcesFromRegistry(tenantId, path);
        if (resource == null) {
            return false;
        }

        return resource.getProperties().containsKey(key);
    }

    private static UserRegistry getTheRegistryInstance(int tenantId) throws RegistryException {
        return ServiceReferenceHolder.getInstance().getRegistryService().getGovernanceSystemRegistry(tenantId);
    }

}
