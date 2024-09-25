package org.wso2.healthcare.consentmgt.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
public class PurposePIIMappingRequestDTO  {
  
  
  
  private Integer mappingId = null;
  
  
  private String appName = null;
  
  
  private List<Integer> selectedPiiAttributes = new ArrayList<Integer>();
  
  
  private List<Integer> mandatoryPiiAttributes = new ArrayList<Integer>();

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("mappingId")
  public Integer getMappingId() {
    return mappingId;
  }
  public void setMappingId(Integer mappingId) {
    this.mappingId = mappingId;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("appName")
  public String getAppName() {
    return appName;
  }
  public void setAppName(String appName) {
    this.appName = appName;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("selectedPiiAttributes")
  public List<Integer> getSelectedPiiAttributes() {
    return selectedPiiAttributes;
  }
  public void setSelectedPiiAttributes(List<Integer> selectedPiiAttributes) {
    this.selectedPiiAttributes = selectedPiiAttributes;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("mandatoryPiiAttributes")
  public List<Integer> getMandatoryPiiAttributes() {
    return mandatoryPiiAttributes;
  }
  public void setMandatoryPiiAttributes(List<Integer> mandatoryPiiAttributes) {
    this.mandatoryPiiAttributes = mandatoryPiiAttributes;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class PurposePIIMappingRequestDTO {\n");
    
    sb.append("  mappingId: ").append(mappingId).append("\n");
    sb.append("  appName: ").append(appName).append("\n");
    sb.append("  selectedPiiAttributes: ").append(selectedPiiAttributes).append("\n");
    sb.append("  mandatoryPiiAttributes: ").append(mandatoryPiiAttributes).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
