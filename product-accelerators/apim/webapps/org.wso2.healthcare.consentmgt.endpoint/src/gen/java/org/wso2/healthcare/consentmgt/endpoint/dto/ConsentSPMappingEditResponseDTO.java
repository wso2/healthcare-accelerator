package org.wso2.healthcare.consentmgt.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class ConsentSPMappingEditResponseDTO  {
  
  
  
  private Integer id = null;
  
  
  private String consumerKey = null;
  
  
  private Integer purposeId = null;
  
  
  private String appName = null;
  
  
  private String purposeName = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("id")
  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("consumerKey")
  public String getConsumerKey() {
    return consumerKey;
  }
  public void setConsumerKey(String consumerKey) {
    this.consumerKey = consumerKey;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("purposeId")
  public Integer getPurposeId() {
    return purposeId;
  }
  public void setPurposeId(Integer purposeId) {
    this.purposeId = purposeId;
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
  @JsonProperty("purposeName")
  public String getPurposeName() {
    return purposeName;
  }
  public void setPurposeName(String purposeName) {
    this.purposeName = purposeName;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsentSPMappingEditResponseDTO {\n");
    
    sb.append("  id: ").append(id).append("\n");
    sb.append("  consumerKey: ").append(consumerKey).append("\n");
    sb.append("  purposeId: ").append(purposeId).append("\n");
    sb.append("  appName: ").append(appName).append("\n");
    sb.append("  purposeName: ").append(purposeName).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
