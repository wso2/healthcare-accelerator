# WSO2 Healthcare API Manager 4.0.0 Accelerator - v${project.version}
  
## Prerequisites:
1. Download WSO2 API Manager product
2. Download the WSO2 Healthcare API Manager Accelerator


## Installation Steps:
1. Extract WSO2 APIM product and update the product. Let's call the installed location `<WSO2_APIM_HOME>`.
2. Extract WSO2 Healthcare APIM Accelerator to `<WSO2_APIM_HOME>`. Let's call it `<WSO2_HC_APIM_ACC_HOME>`.
3. Navigate to `<WSO2_HC_APIM_ACC_HOME>` directory and execute `./bin/merge.sh` command. This will copy the artifacts to the WSO2 APIM and add the required configurations.
4. Navigate to `<WSO2_APIM_HOME>` directory and execute `./bin/wso2server.sh` to start the APIM server with WSO2 Healthcare Accelerator.

## Audit Logs:
Running `merge.sh` script creates a audit log folder in the product home. Structure of it looks like below;

```sh
oh-accelerator
├── backup
│   ├── conf
│   ├── jaggeryapps
│   └── webapps
└── merge_audit.log
```
- `merge_audit.log` will have an audit line per execution of the `merge.sh` script of the accelerator. Each line contains execution date and time, user account and the version of the accelerator. Example log line is below;
```sh
Mon May 31 22:01:55 +0530 2021 - john - WSO2 Healthcare API Manager 4.0.0 Accelerator - v${project.version}
```
- `backup` folder contains the files that were originally there in the APIM product before running the accelerator. Please note that only the last state will be there. 

## U2 Update Tool
By default, U2 update related files are added in the build.
```sh
oh-accelerator
├── LICENSE.txt
│   ├── bin
│   ├── update
```