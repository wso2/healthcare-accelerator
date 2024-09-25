package org.wso2.healthcare.consentmgt.endpoint;

import org.wso2.healthcare.consentmgt.endpoint.dto.AppPurposeRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentPurposeRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PIICategoryAddRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PurposePIIMappingRequestDTO;

import javax.ws.rs.core.Response;

public abstract class ConsentsApiService {
    public abstract Response consentsApplicationsGet(Integer limit,Integer offset,String consumerKey,String appName);
    public abstract Response consentsApplicationsPurposePost(AppPurposeRequestDTO appPurpose);
    public abstract Response consentsPiiCategoriesGet(Integer limit,Integer offset);
    public abstract Response consentsPiiCategoriesPost(PIICategoryAddRequestDTO piiCategory);
    public abstract Response consentsPurposesGet(Integer limit,Integer offset);
    public abstract Response consentsPurposesPiiMappingPost(PurposePIIMappingRequestDTO pirposePIIMapping);
    public abstract Response consentsPurposesPost(ConsentPurposeRequestDTO consentPurpose);
    public abstract Response consentsSpPurposeMappingGet(Integer limit,Integer offset,Integer purposeId,String consumerKey,String appName);
    public abstract Response consentsSpPurposeMappingMappingIdDelete(Integer mappingId,Integer purposeId);
    public abstract Response consentsSpPurposeMappingMappingIdGet(Integer mappingId);
    public abstract Response consentsSpPurposeMappingMappingIdPut(ConsentSPMappingRequestDTO consentSPMapping);
    public abstract Response consentsSpPurposeMappingPost(ConsentSPMappingRequestDTO consentSPMapping);
}

