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
package org.wso2.healthcare.consentmgt.endpoint.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.consent.mgt.core.exception.ConsentManagementException;
import org.wso2.carbon.consent.mgt.core.model.Purpose;
import org.wso2.carbon.consent.mgt.core.model.PurposePIICategory;
import org.wso2.healthcare.consentmgt.core.OHConsentConstants;
import org.wso2.healthcare.consentmgt.core.exception.FHIRConsentMgtException;
import org.wso2.healthcare.consentmgt.endpoint.ConsentsApiService;
import org.wso2.healthcare.consentmgt.endpoint.dto.AppPurposeRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ApplicationsListResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentAddPurposeResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentListPurposeResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentPurposeRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentPurposeResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingAddResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PIIAttributeDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PIIAttributeWithMappingDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PIICategoryAddRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PIICategoryResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PurposeDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PurposePIIMappingRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.impl.util.ConsentEndpointUtils;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_NO_USER_FOUND;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PII_CATEGORY_ALREADY_EXIST;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PII_CATEGORY_ID_INVALID;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_ALREADY_EXIST;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_CATEGORY_ALREADY_EXIST;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_CATEGORY_ID_INVALID;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_PURPOSE_ID_INVALID;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_RECEIPT_ID_INVALID;
import static org.wso2.carbon.consent.mgt.core.constant.ConsentConstants.ErrorMessages.ERROR_CODE_USER_NOT_AUTHORIZED;

public class ConsentsApiServiceImpl extends ConsentsApiService {

    private static final Log LOG = LogFactory.getLog(ConsentsApiServiceImpl.class);

    public Response consentsApplicationsGet(Integer limit, Integer offset, String consumerKey, String appName) {
        try {
            ApplicationsListResponseDTO applications = ConsentEndpointUtils
                    .getApplications(limit, offset, consumerKey, appName);
            return Response.ok().entity(applications).build();
        } catch (FHIRConsentMgtException e) {
            return handleErrorResponse(e);
        }
    }

    public Response consentsApplicationsPurposePost(AppPurposeRequestDTO appPurpose) {
        try {
            Purpose purpose = ConsentEndpointUtils.addAppSpecificPurpose(appPurpose);
            PurposeDTO purposeDTO = new PurposeDTO();
            if (purpose != null) {
                purposeDTO.setId(purpose.getId());
                purposeDTO.setName(purpose.getName());
            }
            return Response.ok(purposeDTO).build();
        } catch (FHIRConsentMgtException e) {
            return handleErrorResponse(e);
        }
    }

    public Response consentsPiiCategoriesGet(Integer limit, Integer offset) {
        try {
            List<org.wso2.healthcare.consentmgt.endpoint.impl.model.PIICategory> piiCategories = ConsentEndpointUtils
                    .getPIICategories(limit, offset);
            PIICategoryResponseDTO responseDTO = new PIICategoryResponseDTO();
            for (org.wso2.healthcare.consentmgt.endpoint.impl.model.PIICategory piiCategory : piiCategories) {
                PIIAttributeWithMappingDTO attributeWithMappingDTO = new PIIAttributeWithMappingDTO();
                PIIAttributeDTO attributeDTO = new PIIAttributeDTO();
                //check if the piicategory name is constructed using fhir path
                if (piiCategory.getPiiCategory().getName().contains("--")) {
                    attributeDTO.setName(piiCategory.getPiiCategory().getName()
                            .substring(0, piiCategory.getPiiCategory().getName().indexOf("--")));
                    if (piiCategory.getPiiCategory().getName().contains("VALUE:")) {
                        String fhirpath = piiCategory.getPiiCategory().getName()
                                .substring(piiCategory.getPiiCategory().getName().indexOf("VALUE:") + 6);
                        attributeDTO.setFhirPath(fhirpath);
                    }
                } else {
                    attributeDTO.setName(piiCategory.getPiiCategory().getName());
                    attributeDTO.setFhirPath(piiCategory.getPiiCategory().getName());
                }
                attributeDTO.setDisplayName(piiCategory.getPiiCategory().getDisplayName());
                attributeDTO.setDescription(piiCategory.getPiiCategory().getDescription());
                attributeWithMappingDTO.setPiiAttribute(attributeDTO);
                attributeWithMappingDTO.setPiiCategoryId(piiCategory.getPiiCategoryId());
                attributeWithMappingDTO.setType(piiCategory.getType());
                responseDTO.getCategories().add(attributeWithMappingDTO);
            }
            return Response.ok().entity(responseDTO).build();
        } catch (FHIRConsentMgtException e) {
            return handleErrorResponse(e);
        }
    }

    public Response consentsPiiCategoriesPost(PIICategoryAddRequestDTO piiCategory) {
        try {
            PIIAttributeDTO attributeDTO = ConsentEndpointUtils.addPIICategory(piiCategory);
            return Response.ok().entity(attributeDTO).build();
        } catch (FHIRConsentMgtException e) {
            return handleErrorResponse(e);
        }
    }

    public Response consentsPurposesGet(Integer limit, Integer offset) {
        try {
            List<Purpose> consentPurposes = ConsentEndpointUtils.getConsentPurposes(limit, offset);
            ConsentListPurposeResponseDTO listPurposeResponseDTO = new ConsentListPurposeResponseDTO();
            for (Purpose consentPurpose : consentPurposes) {
                ConsentPurposeResponseDTO responseDTO = new ConsentPurposeResponseDTO();
                responseDTO.setResource(consentPurpose.getGroup());
                PurposeDTO purposeDTO = new PurposeDTO();
                purposeDTO.setName(consentPurpose.getName());
                purposeDTO.setDescription(consentPurpose.getDescription());
                purposeDTO.setGroup(consentPurpose.getGroup());
                purposeDTO.setGroupType(consentPurpose.getGroupType());
                List<PurposePIICategory> purposePIICategories = consentPurpose.getPurposePIICategories();
                for (PurposePIICategory purposePIICategory : purposePIICategories) {
                    PIIAttributeDTO attributeDTO = new PIIAttributeDTO();
                    attributeDTO.setDescription(purposePIICategory.getDescription());
                    attributeDTO.setDisplayName(purposePIICategory.getDisplayName());
                    attributeDTO.setName(purposePIICategory.getName());
                    attributeDTO.setFhirPath(purposePIICategory.getName());
                    responseDTO.getAttributeList().add(attributeDTO);
                }
                responseDTO.setPurpose(purposeDTO);
                listPurposeResponseDTO.getPurposes().add(responseDTO);
            }
            return Response.ok().entity(listPurposeResponseDTO).build();
        } catch (FHIRConsentMgtException e) {
            return handleErrorResponse(e);
        }
    }

    public Response consentsPurposesPiiMappingPost(PurposePIIMappingRequestDTO purposePIIMapping) {
        try {
            ConsentEndpointUtils.assignMandatoryPIICategoriesToPurpose(purposePIIMapping);
            return Response.ok().build();
        } catch (FHIRConsentMgtException e) {
            return handleErrorResponse(e);
        }
    }

    public Response consentsPurposesPost(ConsentPurposeRequestDTO consentPurpose) {
        try {
            //adding pii category according to the resource
            PIICategoryAddRequestDTO categoryAddRequestDTO = new PIICategoryAddRequestDTO();
            categoryAddRequestDTO.setType("FHIR_RESOURCE");
            PIIAttributeDTO attributeDTO = new PIIAttributeDTO();
            String attributeName = consentPurpose.getResource() + "--TYPE:" + categoryAddRequestDTO.getType() + "--VALUE:"
                    + consentPurpose.getResource();
            if (!ConsentEndpointUtils.isPIICategoryExists(attributeName)
                    && consentPurpose.getAttributeList().size() == 0) {
                attributeDTO.setName(consentPurpose.getResource());
                attributeDTO.setDisplayName("All " + consentPurpose.getResource() + " Data");
                attributeDTO.setDescription("All data attributes of " + consentPurpose.getResource() + " resource");
                attributeDTO.setFhirPath(consentPurpose.getResource());
                categoryAddRequestDTO.setPiiAttribute(attributeDTO);
                ConsentEndpointUtils.addPIICategory(categoryAddRequestDTO);
                consentPurpose.getAttributeList().add(attributeDTO);
            }
            Purpose purpose = ConsentEndpointUtils.addConsentPurpose(consentPurpose);
            //constructing purpose add response
            ConsentAddPurposeResponseDTO responseDTO = new ConsentAddPurposeResponseDTO();
            PurposeDTO purposeDTO = new PurposeDTO();
            purposeDTO.setName(purpose.getName());
            responseDTO.setPurpose(purposeDTO);
            return Response.ok().entity(responseDTO).build();
        } catch (FHIRConsentMgtException e) {
            return handleErrorResponse(e);
        }
    }

    @Override
    public Response consentsSpPurposeMappingGet(Integer limit, Integer offset, Integer purposeId,String consumerKey, String appName){
        try {
            List<ConsentSPMappingResponseDTO> responseDTOList = ConsentEndpointUtils
                    .searchSPPurposes(limit, offset, consumerKey, purposeId, appName);
            return Response.ok().entity(responseDTOList).build();
        } catch (FHIRConsentMgtException e) {
            return handleErrorResponse(e);
        }
    }

    public Response consentsSpPurposeMappingMappingIdDelete(Integer mappingId, Integer purposeId) {
        try {
            ConsentEndpointUtils.deleteSPPurposeMapping(mappingId, purposeId);
            return Response.ok().build();
        } catch (FHIRConsentMgtException e) {
            return handleErrorResponse(e);
        }
    }

    @Override
    public Response consentsSpPurposeMappingMappingIdGet(Integer mappingId) {
        try {
            ConsentSPMappingAddResponseDTO spPurpose = ConsentEndpointUtils.getSPPurposesById(mappingId);
            return Response.ok().entity(spPurpose).build();
        } catch (FHIRConsentMgtException e) {
            return handleErrorResponse(e);
        }
    }

    public Response consentsSpPurposeMappingMappingIdPut(ConsentSPMappingRequestDTO consentSPMapping) {
        return null;
    }

    @Override
    public Response consentsSpPurposeMappingPost(ConsentSPMappingRequestDTO consentSPMapping) {
        try {
            ConsentSPMappingAddResponseDTO responseDTO = ConsentEndpointUtils.addSPPurpose(consentSPMapping);
            return Response.ok().entity(responseDTO).build();
        } catch (FHIRConsentMgtException e) {
            return handleErrorResponse(e);
        }
    }

    private Response handleServerErrorResponse(FHIRConsentMgtException e) {
        throw ConsentEndpointUtils.buildInternalServerErrorException(e.getErrorCode(), LOG, e);
    }

    private Response handleServerErrorResponse(ConsentManagementException e) {
        throw ConsentEndpointUtils.buildInternalServerErrorException(e.getErrorCode(), LOG, e);
    }

    private Response handleErrorResponse(FHIRConsentMgtException e) {
        if (isConflictError(e)) {
            throw ConsentEndpointUtils.buildConflictRequestException(e.getMessage(), e.getErrorCode(), LOG, e);
        }

        if (isNotFoundError(e)) {
            throw ConsentEndpointUtils.buildNotFoundRequestException(e.getMessage(), e.getErrorCode(), LOG, e);
        }

        if (isForbiddenError(e)) {
            throw ConsentEndpointUtils.buildForbiddenException(e.getMessage(), e.getErrorCode(), LOG, e);
        }

        if (isBadRequestError(e)) {
            throw ConsentEndpointUtils.buildBadRequestException(e.getMessage(), e.getErrorCode(), LOG, e);
        }
        throw ConsentEndpointUtils.buildInternalServerErrorException(e.getErrorCode(), LOG, e);
    }

    private boolean isForbiddenError(FHIRConsentMgtException e) {
        return ERROR_CODE_NO_USER_FOUND.getCode().equals(e.getErrorCode()) || ERROR_CODE_USER_NOT_AUTHORIZED.getCode()
                .equals(e.getErrorCode());
    }

    private boolean isNotFoundError(FHIRConsentMgtException e) {
        return ERROR_CODE_PURPOSE_ID_INVALID.getCode().equals(e.getErrorCode())
                || ERROR_CODE_PURPOSE_CATEGORY_ID_INVALID.getCode().equals(e.getErrorCode())
                || ERROR_CODE_PII_CATEGORY_ID_INVALID.getCode().equals(e.getErrorCode())
                || ERROR_CODE_RECEIPT_ID_INVALID.getCode().equals(e.getErrorCode());
    }

    private boolean isConflictError(FHIRConsentMgtException e) {
        return ERROR_CODE_PURPOSE_ALREADY_EXIST.getCode().equals(e.getErrorCode()) ||
                ERROR_CODE_PURPOSE_CATEGORY_ALREADY_EXIST.getCode().equals(e.getErrorCode()) ||
                ERROR_CODE_PII_CATEGORY_ALREADY_EXIST.getCode().equals(e.getErrorCode());
    }

    private boolean isBadRequestError(FHIRConsentMgtException e) {
        return OHConsentConstants.ErrorMessages.ERROR_CODE_MAPPING_ID_UNAVAILABLE.getCode().equals(e.getErrorCode());
    }
}
