package org.wso2.healthcare.consentmgt.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
public class ConsentListPurposeResponseDTO  {
  
  
  
  private List<ConsentPurposeResponseDTO> purposes = new ArrayList<ConsentPurposeResponseDTO>();

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("purposes")
  public List<ConsentPurposeResponseDTO> getPurposes() {
    return purposes;
  }
  public void setPurposes(List<ConsentPurposeResponseDTO> purposes) {
    this.purposes = purposes;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsentListPurposeResponseDTO {\n");
    
    sb.append("  purposes: ").append(purposes).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
