package org.wso2.healthcare.consentmgt.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
public class PIICategoryResponseDTO  {
  
  
  
  private List<PIIAttributeWithMappingDTO> categories = new ArrayList<PIIAttributeWithMappingDTO>();

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("categories")
  public List<PIIAttributeWithMappingDTO> getCategories() {
    return categories;
  }
  public void setCategories(List<PIIAttributeWithMappingDTO> categories) {
    this.categories = categories;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class PIICategoryResponseDTO {\n");
    
    sb.append("  categories: ").append(categories).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
