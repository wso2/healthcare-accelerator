package org.wso2.healthcare.consentmgt.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
public class ConsentPurposeResponseDTO  {
  
  
  
  private String resource = null;
  
  
  private PurposeDTO purpose = null;
  
  
  private List<PIIAttributeDTO> attributeList = new ArrayList<PIIAttributeDTO>();

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("resource")
  public String getResource() {
    return resource;
  }
  public void setResource(String resource) {
    this.resource = resource;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("purpose")
  public PurposeDTO getPurpose() {
    return purpose;
  }
  public void setPurpose(PurposeDTO purpose) {
    this.purpose = purpose;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("attributeList")
  public List<PIIAttributeDTO> getAttributeList() {
    return attributeList;
  }
  public void setAttributeList(List<PIIAttributeDTO> attributeList) {
    this.attributeList = attributeList;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsentPurposeResponseDTO {\n");
    
    sb.append("  resource: ").append(resource).append("\n");
    sb.append("  purpose: ").append(purpose).append("\n");
    sb.append("  attributeList: ").append(attributeList).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
