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

package org.wso2.healthcare.consentmgt.core;

import org.wso2.healthcare.consentmgt.core.dao.ConsentPurposeDAO;
import org.wso2.healthcare.consentmgt.core.dao.ConsumerAppDAO;
import org.wso2.healthcare.consentmgt.core.dao.SPPurposeMappingDAO;
import org.wso2.healthcare.consentmgt.core.dao.impl.ConsentPurposeDAOImpl;
import org.wso2.healthcare.consentmgt.core.dao.impl.ConsumerAppDAOImpl;
import org.wso2.healthcare.consentmgt.core.dao.impl.SPPurposeMappingDAOImpl;
import org.wso2.healthcare.consentmgt.core.exception.FHIRConsentMgtException;
import org.wso2.healthcare.consentmgt.core.model.SPApplicationListResponse;
import org.wso2.healthcare.consentmgt.core.model.SPPurposeMapping;

import java.util.List;

public class ConsentPurposeMapperImpl implements org.wso2.healthcare.consentmgt.core.ConsentPurposeMapper {

    private SPPurposeMappingDAO spPurposeMappingDAO;
    private ConsentPurposeDAO consentPurposeDAO;
    private ConsumerAppDAO consumerAppDAO;

    public ConsentPurposeMapperImpl() {
        spPurposeMappingDAO = new SPPurposeMappingDAOImpl();
        consentPurposeDAO = new ConsentPurposeDAOImpl();
        consumerAppDAO = new ConsumerAppDAOImpl();
    }

    @Override
    public SPPurposeMapping addSPPurpose(SPPurposeMapping spPurposeMapping) throws FHIRConsentMgtException {
        String purposeName = consentPurposeDAO.getPurposeNameFromId(spPurposeMapping.getPurposeId());
        String appName = consumerAppDAO.getAppNameFromKey(spPurposeMapping.getConsumerKey());
        spPurposeMapping.setAppName(appName);
        spPurposeMapping.setPurposeName(purposeName);
        return spPurposeMappingDAO.addSPPurpose(spPurposeMapping);
    }

    @Override
    public SPPurposeMapping updateSPPurpose(SPPurposeMapping spPurposeMapping)
            throws FHIRConsentMgtException {
        return spPurposeMappingDAO.updateSPPurpose(spPurposeMapping);
    }

    @Override
    public SPPurposeMapping getSPPurposesById(Integer id) throws FHIRConsentMgtException {
        return spPurposeMappingDAO.getSPPurposesById(id);
    }

    @Override
    public void deleteSPPurposeById(Integer id) throws FHIRConsentMgtException {
        spPurposeMappingDAO.deleteSPPurposesById(id);
    }

    @Override
    public List<SPPurposeMapping> getSPPurposesByAppId(String consumerKey) throws FHIRConsentMgtException {
        return spPurposeMappingDAO.getSPPurposesByConsumerKey(consumerKey);
    }

    @Override
    public List<SPPurposeMapping> getSPPurposesByAppName(String appName) throws FHIRConsentMgtException {
        return spPurposeMappingDAO.getSPPurposesByAppName(appName);
    }

    @Override
    public List<SPPurposeMapping> getSPPurposesByPurposeId(Integer purposeId) throws FHIRConsentMgtException {
        return spPurposeMappingDAO.getSPPurposesByPurposeId(purposeId);
    }

    @Override
    public List<SPPurposeMapping> searchSPPurposes(Integer limit, Integer offset, String consumerKey,
            Integer purposeId, String applicationName) throws FHIRConsentMgtException {
        return spPurposeMappingDAO.searchSPPurposes(purposeId, consumerKey, limit, offset, applicationName);
    }

    @Override
    public SPApplicationListResponse getApplications(Integer limit, Integer offset, String consumerKey, String appName)
            throws FHIRConsentMgtException {
        return spPurposeMappingDAO.getApplications(limit,offset, consumerKey, appName);
    }
}
