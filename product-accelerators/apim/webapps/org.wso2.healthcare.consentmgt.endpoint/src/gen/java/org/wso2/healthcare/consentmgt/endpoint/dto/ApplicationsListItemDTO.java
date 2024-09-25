package org.wso2.healthcare.consentmgt.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
public class ApplicationsListItemDTO  {
  
  
  
  private String id = null;
  
  
  private String name = null;
  
  
  private List<PurposeItemDTO> purposes = new ArrayList<PurposeItemDTO>();

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("purposes")
  public List<PurposeItemDTO> getPurposes() {
    return purposes;
  }
  public void setPurposes(List<PurposeItemDTO> purposes) {
    this.purposes = purposes;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplicationsListItemDTO {\n");
    
    sb.append("  id: ").append(id).append("\n");
    sb.append("  name: ").append(name).append("\n");
    sb.append("  purposes: ").append(purposes).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
