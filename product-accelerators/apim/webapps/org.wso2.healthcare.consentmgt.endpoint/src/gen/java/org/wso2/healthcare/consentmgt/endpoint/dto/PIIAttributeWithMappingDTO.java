package org.wso2.healthcare.consentmgt.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class PIIAttributeWithMappingDTO  {
  
  
  
  private String type = null;
  
  
  private Integer piiCategoryId = null;
  
  
  private PIIAttributeDTO piiAttribute = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("type")
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("piiCategoryId")
  public Integer getPiiCategoryId() {
    return piiCategoryId;
  }
  public void setPiiCategoryId(Integer piiCategoryId) {
    this.piiCategoryId = piiCategoryId;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("piiAttribute")
  public PIIAttributeDTO getPiiAttribute() {
    return piiAttribute;
  }
  public void setPiiAttribute(PIIAttributeDTO piiAttribute) {
    this.piiAttribute = piiAttribute;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class PIIAttributeWithMappingDTO {\n");
    
    sb.append("  type: ").append(type).append("\n");
    sb.append("  piiCategoryId: ").append(piiCategoryId).append("\n");
    sb.append("  piiAttribute: ").append(piiAttribute).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
