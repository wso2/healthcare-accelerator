package org.wso2.healthcare.consentmgt.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
public class ApplicationsListResponseDTO  {
  
  
  
  private List<ApplicationsListItemDTO> applications = new ArrayList<ApplicationsListItemDTO>();

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("applications")
  public List<ApplicationsListItemDTO> getApplications() {
    return applications;
  }
  public void setApplications(List<ApplicationsListItemDTO> applications) {
    this.applications = applications;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplicationsListResponseDTO {\n");
    
    sb.append("  applications: ").append(applications).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
