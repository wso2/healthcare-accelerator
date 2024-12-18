# WSO2 Healthcare API Manager Accelerator - v${project.version}

The WSO2 Healthcare API Manager Accelerator provides features tailored for healthcare API management, particularly focusing on FHIR (Fast Healthcare Interoperability Resources), SMART on FHIR, and OAuth 2.0 support. 

## Features Overview

### FHIR Capability Statement Auto-Generation
  The FHIR metadata endpoint auto-generates a **CapabilityStatement** that describes the FHIR server's functionality. This endpoint, `https://<API_GW_HOST>:<PORT>/r4/metadata`, provides an essential overview of supported FHIR operations, resource types, and interactions, helping developers and integrators understand the server’s FHIR capabilities.

### OAuth 2.0 Discovery via Well-Known Endpoint
  The well-known endpoint (`https://<API_GW_HOST>:<PORT>/r4/.well-known/smart-configuration`) allows OAuth 2.0 clients to easily discover authentication and token endpoints as well as supported scopes and claims. This simplifies the setup for applications integrating with OAuth 2.0 and OpenID Connect for healthcare data security.

### SMART on FHIR Integration
  SMART on FHIR support enables the integration of applications that use the SMART on FHIR framework. This includes capabilities like SMART scopes registration, SMART App Launch flow features etc.

## Prerequisites:
1. Download WSO2 API Manager product from [WSO2 API Manager](https://apim.docs.wso2.com/en/latest/). 
2. Download the WSO2 Healthcare API Manager Accelerator from [Releases](https://github.com/wso2/healthcare-accelerator/releases) section of the accelerator repository.


## Installation Steps:
1. Extract WSO2 APIM product and update the product. Let's call the installed location `<WSO2_APIM_HOME>`.
2. Extract WSO2 Healthcare APIM Accelerator to `<WSO2_APIM_HOME>`. Let's call it `<WSO2_HC_APIM_ACC_HOME>`.
3. [Optional] Check the accelerator configurations in `<WSO2_HC_APIM_ACC_HOME>/conf/config.toml` file to enable or disable features.
4. Navigate to `<WSO2_HC_APIM_ACC_HOME>` directory and execute `./bin/merge.sh` command. This will copy the artifacts to the WSO2 APIM and add the required configurations.
5. Navigate to `<WSO2_APIM_HOME>` directory and execute `./bin/api-manager.sh` to start the APIM server with WSO2 Healthcare Accelerator.

## Audit Logs:
Running `merge.sh` script creates a audit log folder in the product home. Structure of it looks like below;

```sh
hc-accelerator
├── backup
│   ├── conf
│   ├── jaggeryapps
│   └── webapps
└── merge_audit.log
```
- `merge_audit.log` will have an audit line per execution of the `merge.sh` script of the accelerator. Each line contains execution date and time, user account and the version of the accelerator. Example log line is below;
```sh
Mon May 31 22:01:55 +0530 2021 - john - WSO2 Healthcare API Manager Accelerator - v${project.version}
```
- `backup` folder contains the files that were originally there in the APIM product before running the accelerator. Please note that only the last state will be there. 

## U2 Update Tool
By default, U2 update related files are added in the build.
```sh
hc-accelerator
├── LICENSE.txt
│   ├── bin
│   ├── updates
```