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

package org.wso2.healthcare.consentmgt.endpoint.impl.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.wso2.carbon.consent.mgt.core.ConsentManager;
import org.wso2.carbon.consent.mgt.core.constant.ConsentConstants;
import org.wso2.carbon.consent.mgt.core.exception.ConsentManagementException;
import org.wso2.carbon.consent.mgt.core.model.PIICategory;
import org.wso2.carbon.consent.mgt.core.model.PurposePIICategory;
import org.wso2.carbon.consent.mgt.core.model.ReceiptListResponse;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.healthcare.consentmgt.core.ConsentPurposeMapper;
import org.wso2.healthcare.consentmgt.core.OHConsentConstants;
import org.wso2.healthcare.consentmgt.core.PIICategoryMapper;
import org.wso2.healthcare.consentmgt.core.exception.FHIRConsentMgtException;
import org.wso2.healthcare.consentmgt.core.model.PIICategoryMapping;
import org.wso2.healthcare.consentmgt.core.model.Purpose;
import org.wso2.healthcare.consentmgt.core.model.SPApplication;
import org.wso2.healthcare.consentmgt.core.model.SPApplicationListResponse;
import org.wso2.healthcare.consentmgt.core.model.SPPurposeMapping;
import org.wso2.healthcare.consentmgt.endpoint.dto.AppPurposeRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ApplicationsListItemDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ApplicationsListResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentPurposeRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingAddResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingEditResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ErrorDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PIIAttributeDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PIICategoryAddRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PurposeDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PurposeItemDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PurposePIIMappingRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.impl.exception.BadRequestException;
import org.wso2.healthcare.consentmgt.endpoint.impl.exception.ConflictRequestException;
import org.wso2.healthcare.consentmgt.endpoint.impl.exception.ForbiddenException;
import org.wso2.healthcare.consentmgt.endpoint.impl.exception.InternalServerErrorException;
import org.wso2.healthcare.consentmgt.endpoint.impl.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to define the utilities require for Open Healthcare Consent Management Web App.
 */
public class ConsentEndpointUtils {

    /**
     * This method is used to get consent purpose to application mapper osgi service instance
     *
     * @return ConsentPurposeMapper instance
     */
    public static ConsentPurposeMapper getConsentPurposeMapper() {
        return (ConsentPurposeMapper) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getOSGiService(ConsentPurposeMapper.class, null);
    }

    /**
     * This method is used to get consent manager osgi service instance
     *
     * @return ConsentPurposeMapper instance
     */
    public static ConsentManager getConsentManager() {
        return (ConsentManager) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getOSGiService(ConsentManager.class, null);
    }

    /**
     * This method is used to get pii category mapper osgi service instance
     *
     * @return ConsentPurposeMapper instance
     */
    public static PIICategoryMapper getPIICategoryMapper() {
        return (PIICategoryMapper) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getOSGiService(PIICategoryMapper.class, null);
    }

    /**
     * This method is used to retrieve a SP app to consent purpose mapping based on the consumer key and the purpose id
     *
     * @param requestDTO ConsentSPMappingRequestDTO
     * @return SPPurposeMapping object
     */
    public static SPPurposeMapping getSPPurpose(ConsentSPMappingRequestDTO requestDTO) {
        SPPurposeMapping spPurposeMapping = new SPPurposeMapping();
        spPurposeMapping.setConsumerKey(requestDTO.getConsumerKey());
        spPurposeMapping.setPurposeId(requestDTO.getPurposeId());
        return spPurposeMapping;
    }

    /**
     * This method is used to retrieve a SP app to consent purpose mapping by the mapping id
     *
     * @param id mapping id
     * @return ConsentSPMappingAddResponseDTO object
     * @throws FHIRConsentMgtException If error occurs while retrieving sp purpose by id
     */
    public static ConsentSPMappingAddResponseDTO getSPPurposesById(Integer id) throws FHIRConsentMgtException {
        SPPurposeMapping spPurposeMapping = getConsentPurposeMapper().getSPPurposesById(id);
        ConsentSPMappingAddResponseDTO responseDTO = new ConsentSPMappingAddResponseDTO();
        responseDTO.setId(spPurposeMapping.getId());
        responseDTO.setPurposeId(spPurposeMapping.getPurposeId());
        responseDTO.setConsumerKey(spPurposeMapping.getConsumerKey());
        responseDTO.setAppName(spPurposeMapping.getAppName());
        responseDTO.setPurposeName(spPurposeMapping.getPurposeName());
        return responseDTO;
    }

    /**
     * This method is used to add SP app to consent purpose mapping
     *
     * @param requestDTO ConsentSPMappingRequestDTO
     * @return ConsentSPMappingAddResponseDTO object
     * @throws FHIRConsentMgtException If error occurs while adding consent purpose mapping
     */
    public static ConsentSPMappingAddResponseDTO addSPPurpose(ConsentSPMappingRequestDTO requestDTO)
            throws FHIRConsentMgtException {
        SPPurposeMapping spPurposeMapping = getConsentPurposeMapper().addSPPurpose(getSPPurpose(requestDTO));
        ConsentSPMappingAddResponseDTO responseDTO = new ConsentSPMappingAddResponseDTO();
        responseDTO.setId(spPurposeMapping.getId());
        return responseDTO;
    }

    /**
     * This method is used to update SP app to consent purpose mapping
     *
     * @param requestDTO ConsentSPMappingRequestDTO
     * @return ConsentSPMappingEditResponseDTO object
     * @throws FHIRConsentMgtException If error occurs while updating consent purpose mapping
     */
    public static ConsentSPMappingEditResponseDTO updateSPPurpose(ConsentSPMappingRequestDTO requestDTO)
            throws FHIRConsentMgtException {
        SPPurposeMapping spPurposeMapping = getConsentPurposeMapper().updateSPPurpose(getSPPurpose(requestDTO));
        ConsentSPMappingEditResponseDTO responseDTO = new ConsentSPMappingEditResponseDTO();
        responseDTO.setId(spPurposeMapping.getId());
        return responseDTO;
    }

    /**
     * This method is used to delete a SP app to consent purpose mapping by the mapping id
     *
     * @param id mapping id
     * @throws FHIRConsentMgtException if error occurs while deleting sp purpose mapping
     */
    public static void deleteSPPurposeMapping(Integer id, Integer purposeId)
            throws FHIRConsentMgtException {
        deleteRevokedConsentsForPurposes();
        getConsentPurposeMapper().deleteSPPurposeById(id);
        try {
            getConsentManager().deletePurpose(purposeId);
        } catch (ConsentManagementException e) {
            String msg = "Error occurred while deleting the purpose.";
            throw new FHIRConsentMgtException(msg, e);
        }
    }

    //TODO move to a task
    private static void deleteRevokedConsentsForPurposes(){
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername("admin");
        try {
            List<ReceiptListResponse> revokedReceipts = getConsentManager()
                    .searchReceipts(0, 0, "brian", null, null, "REVOKED");
            for (ReceiptListResponse revokedReceipt : revokedReceipts) {
                getConsentManager().deleteReceipt(revokedReceipt.getConsentReceiptId());
            }
        } catch (ConsentManagementException e) {

        }

        try {
            List<ReceiptListResponse> revokedReceipts = getConsentManager()
                    .searchReceipts(0, 0, "admin", null, null, "REVOKED");
            for (ReceiptListResponse revokedReceipt : revokedReceipts) {
                getConsentManager().deleteReceipt(revokedReceipt.getConsentReceiptId());
            }
        } catch (ConsentManagementException e) {

        }
    }

    /**
     * This method is used to search a SP app to consent purpose mapping
     *
     * @param limit       limit of the search results
     * @param offset      offset of the search results
     * @param consumerKey SP app consumer key
     * @param purposeId   consent purpose id
     * @param appName     SP app name
     * @return List<ConsentSPMappingResponseDTO>
     * @throws FHIRConsentMgtException If error occurs while searching sp purpose.
     */
    public static List<ConsentSPMappingResponseDTO> searchSPPurposes(Integer limit, Integer offset, String consumerKey,
            Integer purposeId, String appName) throws FHIRConsentMgtException {
        List<SPPurposeMapping> spPurposeMappingList = getConsentPurposeMapper()
                .searchSPPurposes(limit, offset, consumerKey, purposeId, appName);
        List<ConsentSPMappingResponseDTO> responseDTOList = new ArrayList<ConsentSPMappingResponseDTO>();
        for (SPPurposeMapping spPurposeMapping : spPurposeMappingList) {
            ConsentSPMappingResponseDTO responseDTO = new ConsentSPMappingResponseDTO();
            responseDTO.setConsumerKey(spPurposeMapping.getConsumerKey());
            responseDTO.setPurposeId(spPurposeMapping.getPurposeId());
            responseDTO.setId(spPurposeMapping.getId());
            responseDTO.setAppName(spPurposeMapping.getAppName());
            responseDTO.setPurposeName(spPurposeMapping.getPurposeName());
            responseDTOList.add(responseDTO);
        }
        return responseDTOList;
    }

    /**
     * This method is used to retrieve SP app to consent purpose mappings with pagination
     *
     * @param limit       limit of the search results
     * @param offset      offset of the search results
     * @param consumerKey SP app consumer key
     * @param appName     SP app name
     * @return ApplicationsListResponseDTO
     * @throws FHIRConsentMgtException If error occurs while fetching sp applications
     */
    public static ApplicationsListResponseDTO getApplications(Integer limit, Integer offset, String consumerKey,
            String appName) throws FHIRConsentMgtException {
        SPApplicationListResponse applicationListResponse = getConsentPurposeMapper()
                .getApplications(limit, offset, consumerKey, appName);
        List<SPApplication> applications = applicationListResponse.getApplications();
        ApplicationsListResponseDTO listResponseDTO = new ApplicationsListResponseDTO();
        for (SPApplication application : applications) {
            List<Purpose> purposes = application.getPurposes();
            ApplicationsListItemDTO applicationsListItemDTO = new ApplicationsListItemDTO();
            for (Purpose purpose : purposes) {
                PurposeItemDTO purposeItemDTO = new PurposeItemDTO();
                purposeItemDTO.setId(purpose.getId());
                purposeItemDTO.setName(purpose.getName());
                applicationsListItemDTO.getPurposes().add(purposeItemDTO);
            }
            applicationsListItemDTO.setId(application.getId());
            applicationsListItemDTO.setName(application.getName());
            listResponseDTO.getApplications().add(applicationsListItemDTO);
        }
        return listResponseDTO;
    }

    /**
     * This method is used to add a OH consent purpose
     *
     * @param consentPurpose ConsentPurposeRequestDTO
     * @return Purpose object
     * @throws FHIRConsentMgtException  If error occurs while adding consent purpose
     */
    public static org.wso2.carbon.consent.mgt.core.model.Purpose addConsentPurpose(
            ConsentPurposeRequestDTO consentPurpose) throws FHIRConsentMgtException {
        PurposeDTO purpose = consentPurpose.getPurpose();
        String group = "CUSTOM_CONSENT";
        if (consentPurpose.getResource() != null) {
            group = consentPurpose.getResource();
        }
        org.wso2.carbon.consent.mgt.core.model.Purpose newConsentPurpose = new org.wso2.carbon.consent.mgt.core.model.Purpose(
                purpose.getName(), purpose.getDescription(), group, "OH_CONSENT_GROUP");
        List<PIIAttributeDTO> attributeList = consentPurpose.getAttributeList();
        try {
            for (PIIAttributeDTO piiAttributeDTO : attributeList) {
                if (getConsentManager().isPIICategoryExists(piiAttributeDTO.getName())) {
                    PIICategory piiCategoryByName = getConsentManager().getPIICategoryByName(piiAttributeDTO.getName());
                    PurposePIICategory purposePIICategory = new PurposePIICategory(piiCategoryByName, false);
                    newConsentPurpose.getPurposePIICategories().add(purposePIICategory);
                }
            }
            return getConsentManager().addPurpose(newConsentPurpose);
        } catch (ConsentManagementException e) {
            throw buildServerException(OHConsentConstants.ErrorMessages.ERROR_CODE_ADD_CONSENT_PURPOSE, null, e);
        }
    }

    /**
     * This method will add consent purpose specific to the application.
     *
     * @param appPurposeRequestDTO {@link AppPurposeRequestDTO}
     * @return matching {@link org.wso2.carbon.consent.mgt.core.model.Purpose}
     * @throws FHIRConsentMgtException If error occurs while adding consent purpose to the app.
     */
    public static org.wso2.carbon.consent.mgt.core.model.Purpose addAppSpecificPurpose(
            AppPurposeRequestDTO appPurposeRequestDTO) throws FHIRConsentMgtException {
        try {
            org.wso2.carbon.consent.mgt.core.model.Purpose purpose = getConsentManager()
                    .getPurpose(appPurposeRequestDTO.getPurposeId());
            String consentGroupType = purpose.getGroupType() + ":" + appPurposeRequestDTO.getAppId();
            purpose.setGroupType(consentGroupType);
            if (!getConsentManager().isPurposeExists(purpose.getName(), purpose.getGroup(), purpose.getGroupType())) {
                return getConsentManager().addPurpose(purpose);
            }
        } catch (ConsentManagementException e) {
            throw buildServerException(OHConsentConstants.ErrorMessages.ERROR_CODE_ADD_APP_SPECIFIC_PURPOSE, null, e);
        }
        return null;
    }

    /**
     * Checks whether the pii category is already existing one
     *
     * @param name pii category name
     * @return boolean result
     */
    public static boolean isPIICategoryExists(String name) {
        try {
            return getConsentManager().isPIICategoryExists(name);
        } catch (ConsentManagementException e) {
            return false;
        }
    }

    /**
     * This method is used to add a PII category
     *
     * @param piiCategory PIICategoryAddRequestDTO
     * @return PIIAttributeDTO
     * @throws FHIRConsentMgtException
     */
    public static PIIAttributeDTO addPIICategory(PIICategoryAddRequestDTO piiCategory) throws FHIRConsentMgtException {
        PIICategoryMapping mapping = new PIICategoryMapping();
        if (StringUtils.isNotBlank(piiCategory.getType())) {
            String name = piiCategory.getPiiAttribute().getName();
            piiCategory.getPiiAttribute().setName(
                    name + "--TYPE:" + piiCategory.getType() + "--VALUE:" + piiCategory.getPiiAttribute()
                            .getFhirPath());
            mapping.setType(piiCategory.getType());
        }
        PIICategory piiCategoryToAdd = new PIICategory(piiCategory.getPiiAttribute().getName(),
                piiCategory.getPiiAttribute().getDescription(), false, piiCategory.getPiiAttribute().getDisplayName());

        try {
            PIICategory addedPiiCategory = ConsentEndpointUtils.getConsentManager().addPIICategory(piiCategoryToAdd);
            PIIAttributeDTO attributeDTO = new PIIAttributeDTO();
            if (addedPiiCategory != null) {
                mapping.setPiiCategoryId(addedPiiCategory.getId());
                ConsentEndpointUtils.getPIICategoryMapper().addPIICategoryMapping(mapping);
                attributeDTO.setName(addedPiiCategory.getName());
                attributeDTO.setDisplayName(addedPiiCategory.getDisplayName());
            }
            return attributeDTO;
        } catch (ConsentManagementException e) {
            throw new FHIRConsentMgtException("Error occurred while adding pii category.", e);
        }
    }

    /**
     * This method is used to retrieve PII categories with the pagination
     *
     * @param limit  limit of the search results
     * @param offset offset of the search results
     * @return List of PIICategory objects
     * @throws FHIRConsentMgtException
     */
    public static List<org.wso2.healthcare.consentmgt.endpoint.impl.model.PIICategory> getPIICategories(
            Integer limit, Integer offset) throws FHIRConsentMgtException {
        try {
            //get all OH pii categories
            List<PIICategoryMapping> piiCategoryMappings = getPIICategoryMapper()
                    .searchPIICategoryMapping(limit, offset, null, null);
            List<org.wso2.healthcare.consentmgt.endpoint.impl.model.PIICategory> oHPIICategories = new ArrayList<org.wso2.healthcare.consentmgt.endpoint.impl.model.PIICategory>();
            for (PIICategoryMapping piiCategoryMapping : piiCategoryMappings) {
                PIICategory piiCategory = getConsentManager().getPIICategory(piiCategoryMapping.getPiiCategoryId());
                org.wso2.healthcare.consentmgt.endpoint.impl.model.PIICategory piiCategoryWithMapping =
                        new org.wso2.healthcare.consentmgt.endpoint.impl.model.PIICategory();
                piiCategoryWithMapping.setPiiCategoryId(piiCategoryMapping.getPiiCategoryId());
                piiCategoryWithMapping.setType(piiCategoryMapping.getType());
                piiCategoryWithMapping.setPiiCategory(piiCategory);
                oHPIICategories.add(piiCategoryWithMapping);
            }
            return oHPIICategories;
        } catch (ConsentManagementException e) {
            throw buildServerException(OHConsentConstants.ErrorMessages.ERROR_CODE_RETRIEVE_PII_CATEGORIES, null, e);
        }
    }

    /**
     * This method is used to retrieve consent purposes with the pagination
     *
     * @param limit  limit of the search results
     * @param offset offset of the search results
     * @return Purpose
     * @throws FHIRConsentMgtException
     */
    public static List<org.wso2.carbon.consent.mgt.core.model.Purpose> getConsentPurposes(Integer limit, Integer offset)
            throws FHIRConsentMgtException {
        try {
            //getting consent purposes for the OH consent group
            if (limit == null) {
                limit = 0;
            }
            if (offset == null) {
                offset = 0;
            }
            List<org.wso2.carbon.consent.mgt.core.model.Purpose> purposes = getConsentManager()
                    .listPurposes(null, "OH_CONSENT_GROUP", limit, offset);
            List<org.wso2.carbon.consent.mgt.core.model.Purpose> purposesWithPIICategories = new ArrayList<org.wso2.carbon.consent.mgt.core.model.Purpose>();
            for (org.wso2.carbon.consent.mgt.core.model.Purpose purpose : purposes) {
                purposesWithPIICategories.add(getConsentManager().getPurpose(purpose.getId()));
            }
            return purposesWithPIICategories;
        } catch (ConsentManagementException e) {
            throw buildServerException(OHConsentConstants.ErrorMessages.ERROR_CODE_RETRIEVE_CONSENT_PURPOSES, null, e);
        }
    }

    /**
     * This method is used to assign mandatory pii categories for a selected consent purpose
     *
     * @param requestDTO PurposePIIMappingRequestDTO
     * @throws FHIRConsentMgtException
     */
    public static void assignMandatoryPIICategoriesToPurpose(PurposePIIMappingRequestDTO requestDTO)
            throws FHIRConsentMgtException {
        Integer mappingId = requestDTO.getMappingId();
        List<Integer> mandatoryPiiAttributes = requestDTO.getMandatoryPiiAttributes();
        List<Integer> selectedPiiAttributes = requestDTO.getSelectedPiiAttributes();
        boolean isUpdateNeeded = false;
        if (mappingId != null) {
            SPPurposeMapping spPurpose = getConsentPurposeMapper().getSPPurposesById(mappingId);
            try {
                org.wso2.carbon.consent.mgt.core.model.Purpose originalPurpose = getConsentManager()
                        .getPurpose(spPurpose.getPurposeId());
                List<PurposePIICategory> purposePIICategories = originalPurpose.getPurposePIICategories();
                List<PurposePIICategory> selectedPurposePIICategories = new ArrayList<PurposePIICategory>();
                for (PurposePIICategory purposePIICategory : purposePIICategories) {
                    if (!selectedPiiAttributes.contains(purposePIICategory.getId())) {
                        isUpdateNeeded = true;
                        continue;
                    } else {
                        selectedPurposePIICategories.add(purposePIICategory);
                    }

                    if (mandatoryPiiAttributes.contains(purposePIICategory.getId())) {
                        if (!purposePIICategory.getMandatory()) {
                            isUpdateNeeded = true;
                            purposePIICategory.setMandatory(true);
                        }
                    } else {
                        if (purposePIICategory.getMandatory() || mandatoryPiiAttributes.size() == 0) {
                            purposePIICategory.setMandatory(false);
                            isUpdateNeeded = true;
                        }
                    }
                }
                originalPurpose.setPurposePIICategories(selectedPurposePIICategories);
                if (isUpdateNeeded) {
                    getConsentPurposeMapper().deleteSPPurposeById(spPurpose.getId());
                    getConsentManager().deletePurpose(originalPurpose.getId());
                    org.wso2.carbon.consent.mgt.core.model.Purpose updatedPurpose = getConsentManager()
                            .addPurpose(originalPurpose);
                    spPurpose.setPurposeId(updatedPurpose.getId());
                    getConsentPurposeMapper().addSPPurpose(spPurpose);
                }

            } catch (ConsentManagementException e) {
                if (ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_IS_ASSOCIATED.getCode()
                        .equals(e.getErrorCode())) {
                    //re adding mapping due to purpose cannot be deleted
                    getConsentPurposeMapper().addSPPurpose(spPurpose);
                    throw buildServerException(OHConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_IS_ASSOCIATED, null, e);
                }
                throw buildServerException(OHConsentConstants.ErrorMessages.ERROR_CODE_UPDATE_PII_CATEGORIES, null, e);
            }
        } else {
            throw buildServerException(OHConsentConstants.ErrorMessages.ERROR_CODE_MAPPING_ID_UNAVAILABLE, null);
        }
    }

    /**
     * This method is used to generate a FHIRConsentMgtException from the OHConsentConstants.ErrorMessages whenever an exception is thrown.
     *
     * @param error OHConsentConstants.ErrorMessages
     * @param data  data to replace if message is needed to be replaced.
     * @param e     Parent exception
     * @return FHIRConsentMgtException
     */
    public static FHIRConsentMgtException buildServerException(OHConsentConstants.ErrorMessages error, String data,
            Throwable e) {
        String message;
        if (StringUtils.isNotBlank(data)) {
            message = String.format(error.getMessage(), data);
        } else {
            message = error.getMessage();
        }
        return new FHIRConsentMgtException(message, error.getCode(), e);
    }

    /**
     * This method is used to generate a FHIRConsentMgtException from the OHConsentConstants.ErrorMessages whenever an exception is thrown.
     *
     * @param error OHConsentConstants.ErrorMessages
     * @param data  data to replace if message is needed to be replaced.
     * @return FHIRConsentMgtException
     */
    public static FHIRConsentMgtException buildServerException(OHConsentConstants.ErrorMessages error, String data) {
        String message;
        if (StringUtils.isNotBlank(data)) {
            message = String.format(error.getMessage(), data);
        } else {
            message = error.getMessage();
        }
        return new FHIRConsentMgtException(message, error.getCode());
    }

    /**
     * This method is used to create an InternalServerErrorException with the known errorCode.
     *
     * @param code Error Code.
     * @return a new InternalServerErrorException with default details.
     */
    public static InternalServerErrorException buildInternalServerErrorException(String code, Log log, Throwable e) {
        ErrorDTO errorDTO = getErrorDTO(ConsentConstants.STATUS_INTERNAL_SERVER_ERROR_MESSAGE_DEFAULT,
                ConsentConstants.STATUS_INTERNAL_SERVER_ERROR_MESSAGE_DEFAULT, code);
        logError(log, e);
        return new InternalServerErrorException(errorDTO);
    }

    /**
     * This method is used to create a BadRequestException with the known errorCode and message.
     *
     * @param description Error Message Desription.
     * @param code        Error Code.
     * @return BadRequestException with the given errorCode and description.
     */
    public static BadRequestException buildBadRequestException(String description, String code, Log log, Throwable e) {
        ErrorDTO errorDTO = getErrorDTO(ConsentConstants.STATUS_BAD_REQUEST_MESSAGE_DEFAULT, description, code);
        logDebug(log, e);
        return new BadRequestException(errorDTO);
    }

    /**
     * This method is used to create a ConflictRequestException with the known errorCode and message.
     *
     * @param description Error Message Description.
     * @param code        Error Code.
     * @return ConflictRequestException with the given errorCode and description.
     */
    public static ConflictRequestException buildConflictRequestException(String description, String code, Log log,
            Throwable e) {
        ErrorDTO errorDTO = getErrorDTO(ConsentConstants.STATUS_BAD_REQUEST_MESSAGE_DEFAULT, description, code);
        logDebug(log, e);
        return new ConflictRequestException(errorDTO);
    }

    /**
     * This method is used to create a NotFoundException with the known errorCode and message.
     *
     * @param description Error Message Description.
     * @param code        Error Code.
     * @return NotFoundException with the given errorCode and description.
     */
    public static NotFoundException buildNotFoundRequestException(String description, String code, Log log,
            Throwable e) {
        ErrorDTO errorDTO = getErrorDTO(ConsentConstants.STATUS_BAD_REQUEST_MESSAGE_DEFAULT, description, code);
        logDebug(log, e);
        return new NotFoundException(errorDTO);
    }

    /**
     * This method is used to create a Forbidden Exception with the known errorCode and message.
     *
     * @param description Error Message Description.
     * @param code        Error Code.
     * @return ForbiddenException with the given errorCode and description.
     */
    public static ForbiddenException buildForbiddenException(String description, String code, Log log, Throwable e) {
        ErrorDTO errorDTO = getErrorDTO(ConsentConstants.STATUS_BAD_REQUEST_MESSAGE_DEFAULT, description, code);
        logDebug(log, e);
        return new ForbiddenException(errorDTO);
    }

    private static ErrorDTO getErrorDTO(String message, String description, String code) {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setCode(code);
        errorDTO.setMessage(message);
        errorDTO.setDescription(description);
        return errorDTO;
    }

    private static void logError(Log log, Throwable throwable) {
        log.error(throwable.getMessage(), throwable);
    }

    private static void logDebug(Log log, Throwable throwable) {
        if (log.isDebugEnabled()) {
            log.debug(ConsentConstants.STATUS_BAD_REQUEST_MESSAGE_DEFAULT, throwable);
        }
    }

}
