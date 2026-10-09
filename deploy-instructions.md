Java 21+
WSO2 IS 7.3.0
WSO2 APIM 4.6.0
wso2-hcam-accelerator-2.0.0
wso2-hcis-accelerator-2.0.0

setup
----
download and update IS and APIM
download and update healthcae accelerators
copy hcam accelerator to apim home
copy hcis accelerator to is home
from product home run
Note if you are runnning a non GA release of HCIS, run with patch flag
- ./wso2-hcis-accelerator-2.0.0/merge.sh --patch
- ./wso2-hcam-accelerator-2.0.0/merge.sh

exchange certificates between IS and APIM

verify the configurations in <product_home>/repository/conf/deployment.toml

deploy extensions
clone https://github.com/wso2/healthcare-accelerator/tree/main/extensions/services and deploy following services
- for each service - consent-app-bff, iam-service-extensions, smart-on-fhir-launch-service
  bal build
  configure Config.toml file referring to Config.toml.example
  run java -jar <service-name>.jar

start apim and is servers
Note: when opening apim server start with flag -DdisableRoleValidationAtScopeCreation=true
log in to wso2 apim admin console and configure identity server 7 as key manager
disable resident key manager

go to publisher portal and create an API

go to devportal and create app
add refresh token, code and client credentials grant types to the app
enable pkce

log in to is console and configure pre issue id token and pre isssue access token extensions to invoke iam service extensions

create user groups patient and practitioner and assign users to these groups and assign roles