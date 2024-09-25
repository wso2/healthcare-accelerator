package org.wso2.healthcare.consentmgt.endpoint;

import io.swagger.annotations.ApiParam;
import org.wso2.healthcare.consentmgt.endpoint.dto.AppPurposeRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ApplicationsListResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentAddPurposeResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentListPurposeResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentPurposeRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingAddResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingEditResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.ConsentSPMappingResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PIIAttributeDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PIICategoryAddRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PIICategoryResponseDTO;
import org.wso2.healthcare.consentmgt.endpoint.dto.PurposePIIMappingRequestDTO;
import org.wso2.healthcare.consentmgt.endpoint.factories.ConsentsApiServiceFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/consents")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@io.swagger.annotations.Api(value = "/consents", description = "the consents API")
public class ConsentsApi  {

   private final ConsentsApiService delegate = ConsentsApiServiceFactory.getConsentsApi();

    @GET
    @Path("/applications")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieve applications defined for consent purposes\n", notes = "This API is used to retrieve applications defined for consent purposes.\n", response = ApplicationsListResponseDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK"),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsApplicationsGet(@ApiParam(value = "Number of search results") @QueryParam("limit")  Integer limit,
    @ApiParam(value = "Start index of the search") @QueryParam("offset")  Integer offset,
    @ApiParam(value = "service provider app id") @QueryParam("consumerKey")  String consumerKey,
    @ApiParam(value = "service provider app name") @QueryParam("appName")  String appName)
    {
    return delegate.consentsApplicationsGet(limit,offset,consumerKey,appName);
    }
    @POST
    @Path("/applications/purpose")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Add pii mappings for a purpose\n", notes = "This API is used to add consent purpose specific to the application.\n", response = void.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 409, message = "Conflict"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsApplicationsPurposePost(@ApiParam(value = "This represents the consent purpose specific to the application." ,required=true ) AppPurposeRequestDTO appPurpose)
    {
    return delegate.consentsApplicationsPurposePost(appPurpose);
    }
    @GET
    @Path("/pii-categories")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieve consent purposes\n", notes = "This API is used to retrieve pii categories.\n", response = PIICategoryResponseDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK"),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsPiiCategoriesGet(@ApiParam(value = "Number of search results") @QueryParam("limit")  Integer limit,
    @ApiParam(value = "Start index of the search") @QueryParam("offset")  Integer offset)
    {
    return delegate.consentsPiiCategoriesGet(limit,offset);
    }
    @POST
    @Path("/pii-categories")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Add a new consent purpose\n", notes = "This API is used to add a new OH pii category.\n", response = PIIAttributeDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 201, message = "Successful response"),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 409, message = "Conflict"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsPiiCategoriesPost(@ApiParam(value = "This represents the consent to sp mapping element that needs to be stored." ,required=true ) PIICategoryAddRequestDTO piiCategory)
    {
    return delegate.consentsPiiCategoriesPost(piiCategory);
    }
    @GET
    @Path("/purposes")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieve consent purposes\n", notes = "This API is used to retrieve consent purposes.\n", response = ConsentListPurposeResponseDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK"),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsPurposesGet(@ApiParam(value = "Number of search results") @QueryParam("limit")  Integer limit,
    @ApiParam(value = "Start index of the search") @QueryParam("offset")  Integer offset)
    {
    return delegate.consentsPurposesGet(limit,offset);
    }
    @POST
    @Path("/purposes/pii-mapping")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Add pii mappings for a purpose\n", notes = "This API is used to add pii mappings for a purpose.\n", response = void.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 409, message = "Conflict"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsPurposesPiiMappingPost(@ApiParam(value = "This represents the consent to sp mapping element that needs to be stored." ,required=true ) PurposePIIMappingRequestDTO pirposePIIMapping)
    {
    return delegate.consentsPurposesPiiMappingPost(pirposePIIMapping);
    }
    @POST
    @Path("/purposes")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Add a new consent purpose\n", notes = "This API is used to add a new consent purpose.\n", response = ConsentAddPurposeResponseDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 201, message = "Successful response"),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 409, message = "Conflict"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsPurposesPost(@ApiParam(value = "This represents the consent to sp mapping element that needs to be stored." ,required=true ) ConsentPurposeRequestDTO consentPurpose)
    {
    return delegate.consentsPurposesPost(consentPurpose);
    }
    @GET
    @Path("/sp-purpose-mapping")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "List service provider app to consent purpose mappings\n", notes = "This API is used to list service provider app to consent purpose mappings.\n", response = ConsentSPMappingResponseDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK"),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsSpPurposeMappingGet(@ApiParam(value = "Number of search results") @QueryParam("limit")  Integer limit,
    @ApiParam(value = "Start index of the search") @QueryParam("offset")  Integer offset,
    @ApiParam(value = "consent purpose id") @QueryParam("purposeId")  Integer purposeId,
    @ApiParam(value = "service provider app id") @QueryParam("consumerKey")  String consumerKey,
    @ApiParam(value = "service provider app name") @QueryParam("appName")  String appName)
    {
    return delegate.consentsSpPurposeMappingGet(limit,offset,purposeId,consumerKey,appName);
    }
    @DELETE
    @Path("/sp-purpose-mapping/{mappingId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Delete a sp consent mapping\n", notes = "This API is used to delete sp consent mapping using the mapping ID.\n", response = void.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK"),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsSpPurposeMappingMappingIdDelete(@ApiParam(value = "The unique identifier of a sp purpose mapping",required=true ) @PathParam("mappingId")  Integer mappingId,
    @ApiParam(value = "consent purpose id") @QueryParam("purposeId")  Integer purposeId)
    {
    return delegate.consentsSpPurposeMappingMappingIdDelete(mappingId,purposeId);
    }
    @GET
    @Path("/sp-purpose-mapping/{mappingId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieve a sp consent mapping\n", notes = "This API is used to retrieve sp consent mapping using the mapping ID.\n", response = ConsentSPMappingResponseDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK"),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsSpPurposeMappingMappingIdGet(@ApiParam(value = "The unique identifier of a sp purpose mapping",required=true ) @PathParam("mappingId")  Integer mappingId)
    {
    return delegate.consentsSpPurposeMappingMappingIdGet(mappingId);
    }
    @PUT
    @Path("/sp-purpose-mapping/{mappingId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Update service provider app to consent purpose mapping\n", notes = "This API is used to update service provider app to consent purpose mapping.\n", response = ConsentSPMappingEditResponseDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 201, message = "Successful response"),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 409, message = "Conflict"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsSpPurposeMappingMappingIdPut(@ApiParam(value = "This represents the consent to sp mapping element that needs to be stored." ,required=true ) ConsentSPMappingRequestDTO consentSPMapping)
    {
    return delegate.consentsSpPurposeMappingMappingIdPut(consentSPMapping);
    }
    @POST
    @Path("/sp-purpose-mapping")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Add service provider app to consent purpose mapping\n", notes = "This API is used to add service provider app to consent purpose mapping.\n", response = ConsentSPMappingAddResponseDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 201, message = "Successful response"),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 401, message = "Unauthorized"),
        
        @io.swagger.annotations.ApiResponse(code = 409, message = "Conflict"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response consentsSpPurposeMappingPost(@ApiParam(value = "This represents the consent to sp mapping element that needs to be stored." ,required=true ) ConsentSPMappingRequestDTO consentSPMapping)
    {
    return delegate.consentsSpPurposeMappingPost(consentSPMapping);
    }
}

