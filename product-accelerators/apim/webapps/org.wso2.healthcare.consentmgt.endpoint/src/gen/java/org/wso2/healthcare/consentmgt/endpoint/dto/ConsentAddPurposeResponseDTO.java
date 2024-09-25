package org.wso2.healthcare.consentmgt.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class ConsentAddPurposeResponseDTO  {
  
  
  
  private String resource = null;
  
  
  private PurposeDTO purpose = null;

  
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

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsentAddPurposeResponseDTO {\n");
    
    sb.append("  resource: ").append(resource).append("\n");
    sb.append("  purpose: ").append(purpose).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
