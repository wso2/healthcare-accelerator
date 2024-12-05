#!/bin/bash
# ------------------------------------------------------------------------
#
# Copyright (c) 2024, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
#
# This software is the property of WSO2 Inc. and its suppliers, if any.
# Dissemination of any information or reproduction of any material contained
# herein is strictly forbidden, unless permitted by WSO2 in accordance with
# the WSO2 Commercial License available at http://wso2.com/licenses. For specific
# language governing the permissions and limitations under this license,
# please see the license as well as any agreement you’ve entered into with
# WSO2 governing the purchase of this software and any associated services.
#
# ------------------------------------------------------------------------

# merge.sh script copy the WSO2 OH APIM accelerator artifacts on top of WSO2 APIM base product
#
# merge.sh <WSO2_OH_APIM_HOME>

WSO2_OH_APIM_HOME=$1

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")/"$link"
  fi
done

# Get standard environment variables
PRGDIR=$(dirname "$PRG")

ACCELERATOR_HOME=$(cd "$PRGDIR/.." || exit ; pwd)

echo "[INFO] Accelerator home is: ${ACCELERATOR_HOME}"

# set product home
if [ "${WSO2_OH_APIM_HOME}" == "" ];
  then
    WSO2_OH_APIM_HOME=${ACCELERATOR_HOME}/..
fi

echo "[INFO] Product home is: ${WSO2_OH_APIM_HOME}"

# validate product home
if [ ! -d "${WSO2_OH_APIM_HOME}/repository/components" ]; then
  echo -e "[ERROR] Specified product path is not a valid carbon product path";
  exit 2;
else
  echo -e "[INFO] Valid carbon product path found";
fi

# Reading the config.toml file
config_toml_file="${ACCELERATOR_HOME}"/conf/config.toml

# Read values from the TOML file]
healthcare_theme_enabled=$(grep -E '^enable_healthcare_theme = ' "$config_toml_file" | cut -d'=' -f2 | tr -d ' ')

metadata_ep_enabled=$(grep -E '^enable_fhir_metadata_endpoint = ' "$config_toml_file" | cut -d'=' -f2 | tr -d ' ')
well_known_ep_enabled=$(grep -E '^enable_well_known_endpoint = ' "$config_toml_file" | cut -d'=' -f2 | tr -d ' ')
smart_on_fhir_enabled=$(grep -E '^enable_smart_on_fhir = ' "$config_toml_file" | cut -d'=' -f2 | tr -d ' ')
developer_workflow_enabled=$(grep -E '^enable_developer_workflow = ' "$config_toml_file" | cut -d'=' -f2 | tr -d ' ')

# create the hc-accelerator folder in product home, if not exist
WSO2_OH_ACCELERATOR_VERSION=$(cat "${ACCELERATOR_HOME}"/version.txt)
WSO2_OH_ACCELERATOR_AUDIT="${WSO2_OH_APIM_HOME}"/hc-accelerator
WSO2_OH_ACCELERATOR_AUDIT_BACKUP="${WSO2_OH_ACCELERATOR_AUDIT}"/backup
if [ ! -d  "${WSO2_OH_ACCELERATOR_AUDIT}" ]; then
   mkdir -p "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"
   mkdir -p "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"/jaggeryapps
   mkdir -p "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"/webapps
   mkdir -p "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"/conf
   echo -e "[INFO] Accelerator audit folder [""${WSO2_OH_ACCELERATOR_AUDIT}""] is created";
else
   echo -e "[INFO] Accelerator audit folder is present at [""${WSO2_OH_ACCELERATOR_AUDIT}""]";
fi

# backup original product files to the audit folder
echo -e "[INFO] Backup original product files.."
if [ -z "$(find "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}/webapps" -mindepth 1 -print -quit)" ]; then
  cp -R "${WSO2_OH_APIM_HOME}"/repository/deployment/server/webapps/accountrecoveryendpoint/ "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"/webapps/accountrecoveryendpoint 2>/dev/null
  cp -R "${WSO2_OH_APIM_HOME}"/repository/deployment/server/webapps/authenticationendpoint/ "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"/webapps/authenticationendpoint 2>/dev/null
  cp "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"/conf 2>/dev/null
  cp "${WSO2_OH_APIM_HOME}"/repository/conf/claim-config.xml "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"/conf 2>/dev/null
else
  echo -e "[INFO] Backup files already exist in the audit folder"
fi

echo -e "[INFO] Copying Open Healthcare artifacts.."
# adding the OH artifacts to the product pack
cp -R "${ACCELERATOR_HOME}"/carbon-home/repository/components/* "${WSO2_OH_APIM_HOME}"/repository/components
cp -R "${ACCELERATOR_HOME}"/carbon-home/repository/resources/* "${WSO2_OH_APIM_HOME}"/repository/resources
cp -R "${ACCELERATOR_HOME}"/carbon-home/repository/deployment/server/synapse-configs/* "${WSO2_OH_APIM_HOME}"/repository/deployment/server/synapse-configs
if [ "${healthcare_theme_enabled}" == "true" ]; then
  cp -R "${ACCELERATOR_HOME}"/carbon-home/repository/deployment/server/webapps/* "${WSO2_OH_APIM_HOME}"/repository/deployment/server/webapps/
else
  cp -R "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"/webapps/* "${WSO2_OH_APIM_HOME}"/repository/deployment/server/webapps/
fi

if [ "${metadata_ep_enabled}" == "false" ]; then
  find "${WSO2_OH_APIM_HOME}/repository/components/dropins" -type f -name "*org.wso2.healthcare.apim.conformance*" -exec rm -f {} \;
  find "${WSO2_OH_APIM_HOME}/repository/components/dropins" -type f -name "*org.wso2.healthcare.apim.multitenancy*" -exec rm -f {} \;
fi

if [ "${smart_on_fhir_enabled}" == "false" ]; then
  find "${WSO2_OH_APIM_HOME}/repository/components" -type f -name "*org.wso2.healthcare.apim.scopemgt*" -exec rm -f {} \;
  find "${WSO2_OH_APIM_HOME}/repository/components" -type f -name "*org.wso2.healthcare.apim.tokenmgt*" -exec rm -f {} \;
fi

if [ "${developer_workflow_enabled}" == "false" ]; then
  find "${WSO2_OH_APIM_HOME}/repository/components/lib" -type f -name "*org.wso2.healthcare.apim.workflow.extensions*" -exec rm -f {} \;
fi

# adding configurations to deployment.toml file
echo -e "[INFO] Adding configurations to deployment.toml file"

if grep -Fxq "[healthcare.fhir]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
then
    # code if found
    echo -e "[WARN] healthcare.fhir configuration already exist"
else
    # code if not found
    echo -e "\n[healthcare.fhir]\nserver_name = \"WSO2 Open Healthcare\"\nserver_version = \"1.2.0\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
fi

if grep -Fxq "[apim.jwt]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
then
    # code if found
    echo -e "[WARN] apim.jwt configuration already exist"
else
    # code if not found
    echo -e "\n[apim.jwt]\nenable = true\nencoding = \"base64\" # base64,base64url\nclaim_dialect = \"http://wso2.org/claims\"\nconvert_dialect = false\nheader = \"X-JWT-Assertion\"\nsigning_algorithm = \"SHA256withRSA\"\nenable_user_claims = true\nclaims_extractor_impl = \"org.wso2.carbon.apimgt.impl.token.ExtendedDefaultClaimsRetriever\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
fi

if [ "${smart_on_fhir_enabled}" == "true" ]; then
  if grep -Fxq "[apim.oauth_config]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] apim.oauth_config configuration already exist"
  else
      # code if not found
      echo -e "\n[apim.oauth_config]\n#enable_outbound_auth_header = false\n#auth_header = \"Authorization\"\n#revoke_endpoint = \"https://localhost:\${https.nio.port}/revoke\"\n#enable_token_encryption = false\n#enable_token_hashing = false\nallowed_scopes = [\"^device_.*\", \"openid\", \"fhirUser\", \"launch/patient\", \"offline_access\"]"  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  if grep -Fxq "[apim.jwt.gateway_generator]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] apim.jwt.gateway_generator configuration already exist"
  else
      # code if not found
      echo -e "\n[apim.jwt.gateway_generator]\nimpl = \"org.wso2.healthcare.apim.gateway.security.jwt.generator.HealthcareGatewayJWTGenerator\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  if grep -Fxq "[oauth]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] oauth configuration already exist"
  fi

  if grep -Fxq "[oauth.grant_type.authorization_code]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] oauth.grant_type.authorization_code configuration already exist"
  else
      # code if not found
      echo -e "\n[oauth.grant_type.authorization_code]\ngrant_handler = \"org.wso2.healthcare.apim.tokenmgt.handlers.OpenHealthcareExtendedAuthorizationCodeGrantHandler\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  # custom code response type handler
  if grep -Fxq "[[oauth.custom_response_type]]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] oauth.custom_response_type configuration already exist"
  else
      # code if not found
      echo -e "\n[[oauth.custom_response_type]]\nname =\"code\"\nclass = \"org.wso2.healthcare.apim.scopemgt.handlers.OpenHealthcareExtendedCodeResponseTypeHandler\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  # custom key validation handler
  if grep -Fxq "[apim.key_manager]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
    # code if found
    echo -e "[WARN] apim.key_manager active configuration already exist"
  else
    # code if not found
    echo -e "\n[apim.key_manager]\nkey_validation_handler_impl = \"org.wso2.healthcare.apim.scopemgt.handlers.OpenHealthcareExtendedKeyValidationHandler\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  # scopemgt config
  if grep -Fxq "#[healthcare.identity.scopemgt]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "[healthcare.identity.scopemgt]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] healthcare.identity.scopemgt configuration already exist"
  else
      # code if not found
      echo -e "\n[healthcare.identity.scopemgt]\nroles = [\"patient-read\", \"patient-write\", \"user-read\", \"user-write\"]\nenable_fhir_scope_to_wso2_scope_mapping = true"  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  # shared scopes
  if grep -Fxq "#[[healthcare.identity.scopemgt.shared_scopes]]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "[[healthcare.identity.scopemgt.shared_scopes]]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] healthcare.identity.scopemgt.shared_scopes configuration already exist"
  else
      # code if not found
     echo -e "\n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"patient/*.c\"\nname = \"patient/*.c\"\nroles = \"patient-write\"\ndescription = \"This scope grants patients access to CREATE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"patient/*.r\"\nname = \"patient/*.r\"\nroles = \"patient-write,patient-read\"\ndescription = \"This scope grants patients access to READ any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"patient/*.u\"\nname = \"patient/*.u\"\nroles = \"patient-write\"\ndescription = \"This scope grants patients access to UPDATE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"patient/*.d\"\nname = \"patient/*.d\"\nroles = \"patient-write\"\ndescription = \"This scope grants patients access to DELETE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"patient/*.s\"\nname = \"patient/*.s\"\nroles = \"patient-write,patient-read\"\ndescription = \"This scope grants patients access to SEARCH any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"user/*.c\"\nname = \"user/*.c\"\nroles = \"user-write\"\ndescription = \"This scope grants other users access to CREATE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"user/*.r\"\nname = \"user/*.r\"\nroles = \"user-write,user-read\"\ndescription = \"This scope grants other users access to READ any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"user/*.u\"\nname = \"user/*.u\"\nroles = \"user-write\"\ndescription = \"This scope grants other users access to UPDATE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"user/*.d\"\nname = \"user/*.d\"\nroles = \"user-write\"\ndescription = \"This scope grants other users access to DELETE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"user/*.s\"\nname = \"user/*.s\"\nroles = \"user-write,user-read\"\ndescription = \"This scope grants other users access to SEARCH any fhir resource.\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  if grep -Fxq "#[healthcare.identity.claims]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "[healthcare.identity.claims]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] healthcare.identity.claims configuration already exist"
  else
      # code if not found
      echo -e "\n#[healthcare.identity.claims]\n#patient_id_claim_uri = \"http://wso2.org/claims/patientId\"\n#patient_id_key = \"patientId\"\n#fhirUser_resource_url_context = \"/r4/Patient\"\n#fhirUser_resource_id_claim_uri = \"http://wso2.org/claims/patientId\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  if grep -Fxq "#[[healthcare.backend.auth]]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "[[healthcare.backend.auth]]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
    then
        # code if found
        echo -e "[WARN] healthcare.backend.auth configuration already exist"
    else
        # code if not found
        echo -e "\n#[[healthcare.backend.auth]]\n## Name of the authentication method. This name must be matched with the Config Name policy attribute in the -\n##  - Replace Backend Auth Token policy.\n#name = \"epic_pkjwt\"\n## Authentication type. Only pkjwt and client_credentials are supported atm.\n#auth_type = \"pkjwt\"\n## External Auth server's Token endpoint URL.\n#token_endpoint = \"https://localhost:9443/oauth2/token\"\n#client_id = \"client_id\"\n#private_key_alias = \"key_alias\"
     \n#[[healthcare.backend.auth]]\n#name = \"epic_client_credentials\"\n#auth_type = \"client_credentials\"\n#token_endpoint = \"https://localhost:9443/oauth2/token\"\n#client_id = \"client_id\"\n#client_secret = \"client_secret\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
    fi

  if grep -Fxq "#[healthcare.identity.claim.mgt]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "[healthcare.identity.claim.mgt]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] healthcare.identity.claim.mgt configuration already exist"
  else
      # code if not found
      echo -e "\n#[healthcare.identity.claim.mgt]\n#enable = false"  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi
fi
if [ "${healthcare_theme_enabled}" == "true" ]; then
  if grep -Fxq "#[healthcare.deployment.webapps]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "[healthcare.deployment.webapps]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] healthcare.deployment.webapps configuration already exist"
  else
      # code if not found
      echo -e "\n#[healthcare.deployment.webapps]\n#name = \"Open Healthcare\"\n#name_container_css = \"\"\n#terms_of_use = \"https://wso2.com/terms-of-use\"\n#privacy_policy = \"https://wso2.com/privacy-policy\"\n#cookie_policy = \"https://wso2.com/cookie-policy\"\n#logo = \"images/wso2-logo.svg\"\n#logoHeight = \"\"\n#logoWidth = \"\"\n#logo_container_css = \"\"\n#title = \"WSO2 Open Healthcare\"\n#favicon = \"libs/theme/assets/images/favicon.ico\"\n#footer = \"WSO2 Open Healthcare\"\n#footer_secondary_html = \"\"\n#main_color = \"#342382\"" | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  if grep -Fxq "#[healthcare.deployment.webapps.authenticationendpoint]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "[healthcare.deployment.webapps.authenticationendpoint]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] healthcare.deployment.webapps.authenticationendpoint configuration already exist"
  else
      # code if not found
      echo -e "\n#[healthcare.deployment.webapps.authenticationendpoint]\n#enable_selfsignup = true\n#selfsignup_url = \"\"\n# Name of deployment to mentioned in the poicy disclaimer.\n# The result will be : Usage of <signin_disclaimer_portal_name> is subject to the Terms of Use, Privacy Policy, and Cookie Policy.\n#signin_disclaimer_portal_name=\"WSO2 Open Healthcare\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  if grep -Fxq "#[healthcare.deployment.webapps.accountrecovery]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "[healthcare.deployment.webapps.accountrecovery]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] healthcare.deployment.webapps.accountrecovery configuration already exist"
  else
      # code if not found
      echo -e "\n#[healthcare.deployment.webapps.accountrecovery]\n#signup_flow.username_info_enable = false\n# Message to display at the end of successful self sign-up\n#signup_flow.success_message=\"User registration completed successfully\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  if grep -Fxq "#[[apim.devportal.application_attributes]]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "[[apim.devportal.application_attributes]]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] apim.devportal.application_attributes configuration already exist"
  else
      # code if not found
      echo -e "\n#[[apim.devportal.application_attributes]]\n#required=true\n#hidden=false\n#name=\"Terms and Conditions Secure URL\"\n#description=\"Provide a secure URL where users can review your terms and conditions prior to authorizing access to their data.\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
  fi
fi

if grep -Fxq "#[apim.sync_runtime_artifacts.gateway.skip_list]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "[apim.sync_runtime_artifacts.gateway.skip_list]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
then
    # code if found
    echo -e "[WARN] apim.sync_runtime_artifacts.gateway.skip_list already exist"
else
    # code if not found
    echo -e "\n[apim.sync_runtime_artifacts.gateway.skip_list]\napis = [\"_MetadataAPI_.xml\",\"_WellKnownResourceAPI_.xml\"]" | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
fi

if grep -Fxq "id = \"private_key_jwt_authenticator\"" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "#id = \"private_key_jwt_authenticator\"" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
then
    # code if found
    echo -e "[WARN] private_key_jwt_authenticator configuration already exists"
else
    # code if not found
    echo -e "\n[[event_listener]]\nid = \"private_key_jwt_authenticator\"\ntype = \"org.wso2.carbon.identity.core.handler.AbstractIdentityHandler\"\nname = \"org.wso2.healthcare.apim.clientauth.jwt.PrivateKeyJWTClientAuthenticator\"\norder = \"899\"\n#[event_listener.properties]\n#max_allowed_jwt_lifetime_seconds = \"300\"\n#token_endpoint_alias = \"sampleurl\"\n\n[[cache.manager]] \nname = \"IdentityApplicationManagementCacheManager\" \ntimeout = \"10\"\ncapacity = \"5000\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
fi

if grep -Fxq "[healthcare.organization]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "#[healthcare.organization]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
then
    # code if found
    echo -e "[WARN] healthcare.organization configuration already exists"
else
    # code if not found
    echo -e "\n#[healthcare.organization]\n#org_name = \"testOrg\"\n#contact_email = \"support@testorg.com\"\n#timezone = \"GMT\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
fi

if grep -Fxq "[[healthcare.notification.mail]]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml || grep -Fxq "#[[healthcare.notification.mail]]" "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
then
    # code if found
    echo -e "[WARN] healthcare.notification.mail configuration already exists"
else
    # code if not found
    echo -e "\n#[[healthcare.notification.mail]]\n#name = \"new_user_signup_requested_internal\"\n#enable = true\n#recipient_roles = \"approver,approver2\"\n#recipients = \"user1@test.com,user2@test.com\"\n#email_subject = \"[Developer Portal]  New User Signup - \${first_name} \${last_name}\"\n#email_body = \"<html><body>A new user has signed up at \${time} [\${timezone}] on \${date}. Visit the <a href=\\\"\${server_url}/admin/tasks/user-creation/\\\">admin portal</a> to approve/reject.</body></html>\"\n\n#[[healthcare.notification.mail]]\n#name = \"new_user_signup_completed_internal\"\n#enable = true\n#recipient_roles = \"approver,approver2\"\n#recipients = \"user1@test.com,user2@test.com\"\n#email_subject = \"[Developer Portal]  New User Signup - \${first_name} \${last_name}\"\n#email_body = \"<html><body>Signup request has been \${status} by \${approver}.</body></html>\"\n\n#[[healthcare.notification.mail]]\n#name = \"new_user_signup_completed_external\"\n#enable = true\n#email_subject = \"[\${org_name} Developer Portal] Your Signup Request Status\"\n#email_body = \"<html><body>Your signup request has been \${status}. Please email \${contact_email} if you have any questions.<br/>Thank you for your interest.<br/></body></html>\"\n\n#[[healthcare.notification.mail]]\n#name = \"new_app_creation_requested_internal\"\n#enable = true\n#recipient_roles = \"approver,approver2\"\n#recipients = \"user1@test.com,user2@test.com\"\n#email_subject = \"[Developer Portal] New Application \${app_name} Created by \${user_name}\"\n#email_body = \"<html><body>A new application has been created. Visit the <a href=\\\"\${server_url}/admin/tasks/application-creation/\\\">admin portal</a> to approve/reject.</body></html>\"\n\n#[[healthcare.notification.mail]]\n#name = \"new_app_creation_completed_internal\"\n#enable = true\n#recipient_roles = \"approver,approver2\"\n#recipients = \"user1@test.com,user2@test.com\"\n#email_subject = \"[Developer Portal] New Application \${app_name} Created by \${user_name}\"\n#email_body = \"<html><body>Application creation request has been \${status} by \${approver}.</body></html>\"\n\n#[[healthcare.notification.mail]]\n#name = \"new_app_creation_completed_external\"\n#enable = true\n#email_subject = \"[\${org_name} Developer Portal] Your Application Creation Request Status\"\n#email_body = \"<html><body>Your request to create the application \${app_name} has been \${status}. Please email \${contact_email} if you have any questions.<br/>Thank you for your interest.<br/></body></html>\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
fi

MATCH_FOUND=$(sed -E '/./{H;$!d;} ; x ; s/\[\[custom_message_formatters\]\](.*)content_type = \"application\/fhir\+xml\"/fhir-xml-formatter-found/g' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml)

# checking whether there's any entry for application/fhir+xml formatter
if grep -q "fhir-xml-formatter-found" <<< "$MATCH_FOUND";
then
  # if so, we need to replace those entries if they are related to OH message formatters - here we are using a multiline sed matching operation
	sed -E '/./{H;$!d;} ; x ; s/\[\[custom_message_formatters\]\](.*)content_type = \"application\/fhir\+xml\"/\[\[custom_message_formatters\]\]\nclass = \"org.apache.axis2.transport.http.ApplicationXMLFormatter\"\ncontent_type = \"application\/fhir\+xml\"/g' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml > "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1

  # above multiline sed operation add an empty line at the begining of the file. Below command will remove that unnecessary line
	sed '1{/^$/d;}' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1 > "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.2
  # replace the deployment.toml with the changed one
  mv "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.2 "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  # clean up
  rm "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1
else
  # adds the fhir+xml specific message formatter
  echo -e "\n[[custom_message_formatters]]\nclass = \"org.apache.axis2.transport.http.ApplicationXMLFormatter\"\ncontent_type = \"application/fhir+xml\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
fi

MATCH_FOUND=$(sed -E '/./{H;$!d;} ; x ; s/\[\[custom_message_builders\]\](.*)content_type = \"application\/fhir\+xml\"/fhir-xml-builder-found/g' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml)

# checking whether there's any entry for application/fhir+xml builder
if grep -q "fhir-xml-builder-found" <<< "$MATCH_FOUND";
then
  # if so, we need to replace those entries if they are related to OH message builders - here we are using a multiline sed matching operation
	sed -E '/./{H;$!d;} ; x ; s/\[\[custom_message_builders\]\](.*)content_type = \"application\/fhir\+xml\"/\[\[custom_message_builders\]\]\nclass = \"org.apache.axis2.builder.ApplicationXMLBuilder\"\ncontent_type = \"application\/fhir\+xml\"/g' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml > "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1

  # above multiline sed operation add an empty line at the begining of the file. Below command will remove that unnecessary line
	sed '1{/^$/d;}' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1 > "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.2
  # replace the deployment.toml with the changed one
  mv "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.2 "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  # clean up
  rm "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1
else
  # adds the fhir+xml specific message builder
  echo -e "\n[[custom_message_builders]]\nclass = \"org.apache.axis2.builder.ApplicationXMLBuilder\"\ncontent_type = \"application/fhir+xml\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
fi

MATCH_FOUND=$(sed -E '/./{H;$!d;} ; x ; s/\[\[custom_message_formatters\]\](.*)content_type = \"application\/fhir\+json\"/fhir-json-formatter-found/g' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml)

# checking whether there's any entry for application/fhir+json formatter
if grep -q "fhir-json-formatter-found" <<< "$MATCH_FOUND";
then
  # if so, we need to replace those entries if they are related to OH message formatters - here we are using a multiline sed matching operation
	sed -E '/./{H;$!d;} ; x ; s/\[\[custom_message_formatters\]\](.*)content_type = \"application\/fhir\+json\"/\[\[custom_message_formatters\]\]\nclass = \"org.apache.synapse.commons.json.JsonStreamFormatter\"\ncontent_type = \"application\/fhir\+json\"/g ' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml > "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1

  # above multiline sed operation add an empty line at the begining of the file. Below command will remove that unnecessary line
	sed '1{/^$/d;}' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1 > "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.2
  # replace the deployment.toml with the changed one
  mv "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.2 "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  # clean up
  rm "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1
else
  # adds the fhir+json specific message formatter
  echo -e "\n[[custom_message_formatters]]\nclass = \"org.apache.synapse.commons.json.JsonStreamFormatter\"\ncontent_type = \"application/fhir+json\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
fi

MATCH_FOUND=$(sed -E '/./{H;$!d;} ; x ; s/\[\[custom_message_builders\]\](.*)content_type = \"application\/fhir\+json\"/fhir-json-builder-found/g' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml)

# checking whether there's any entry for application/fhir+json builder
if grep -q "fhir-json-builder-found" <<< "$MATCH_FOUND";
then
  # if so, we need to replace those entries if they are related to OH message builders - here we are using a multiline sed matching operation
	sed -E '/./{H;$!d;} ; x ; s/\[\[custom_message_builders\]\](.*)content_type = \"application\/fhir\+json\"/\[\[custom_message_builders\]\]\nclass = \"org.apache.synapse.commons.json.JsonStreamBuilder\"\ncontent_type = \"application\/fhir\+json\"/g' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml > "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1

  # above multiline sed operation add an empty line at the begining of the file. Below command will remove that unnecessary line
	sed '1{/^$/d;}' "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1 > "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.2
  # replace the deployment.toml with the changed one
  mv "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.2 "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml
  # clean up
  rm "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml.bak.1
else
  # adds the fhir+json specific message builder
  echo -e "\n[[custom_message_builders]]\nclass = \"org.apache.synapse.commons.json.JsonStreamBuilder\"\ncontent_type = \"application/fhir+json\""  | tee -a "${WSO2_OH_APIM_HOME}"/repository/conf/deployment.toml >/dev/null
fi


# adding configurations to claim-config.xml
echo -e "[INFO] Adding configurations to repository/conf/claim-config.xml file"

PATIENT_ID_CLAIM="\t<Dialect dialectURI=\"http://wso2.org/claims\">\n<Claim>\n<ClaimURI>http://wso2.org/claims/patientId</ClaimURI>\n<DisplayName>Patient ID</DisplayName>\n<AttributeID>patientId</AttributeID>\n<Description>PatientID</Description>\n<DisplayOrder>13</DisplayOrder>\n<SupportedByDefault />\n</Claim>\n"
if grep -Fq '<ClaimURI>http://wso2.org/claims/patientId</ClaimURI>' "${WSO2_OH_APIM_HOME}"/repository/conf/claim-config.xml
then
    # do nothing
    echo -e "[WARN] PatientId local claim configuration already exist"
else
    sed -i -e "s|<Dialect dialectURI=\"http://wso2.org/claims\">|${PATIENT_ID_CLAIM}|g" "${WSO2_OH_APIM_HOME}"/repository/conf/claim-config.xml
fi

PATIENT_ID_OIDC_CLAIM="\t<Dialect dialectURI=\"http://wso2.org/oidc/claim\">\n<Claim>\n<ClaimURI>patientId</ClaimURI>\n<DisplayName>Patient ID</DisplayName>\n<AttributeID>patientId</AttributeID>\n<Description>PatientID</Description>\n<DisplayOrder>13</DisplayOrder>\n<MappedLocalClaim>http://wso2.org/claims/patientId</MappedLocalClaim>\n</Claim>\n"
if grep -Fq '<MappedLocalClaim>http://wso2.org/claims/patientId</MappedLocalClaim>' "${WSO2_OH_APIM_HOME}"/repository/conf/claim-config.xml
then
    # do nothing
    echo -e "[WARN] PatientId OIDC claim configuration already exist"
else
    sed -i -e "s|<Dialect dialectURI=\"http://wso2.org/oidc/claim\">|${PATIENT_ID_OIDC_CLAIM}|g" "${WSO2_OH_APIM_HOME}"/repository/conf/claim-config.xml
fi

# adding configurations to oidc-scope-config.xml
echo -e "[INFO] Adding configurations to repository/conf/identity/oidc-scope-config.xml file"
# adds fhirUser
FHIRUSER_SCOPE="<Scopes>\n\t<Scope id=\"fhirUser\">\n\t\t<Claim>patientId</Claim>\n\t</Scope>\n"
if grep -Fq '<Scope id="fhirUser">' "${WSO2_OH_APIM_HOME}"/repository/conf/identity/oidc-scope-config.xml
then
    # do nothing
    echo -e "[WARN] fhirUser scope configuration already exist"
else
    sed -i -e "s|<Scopes>|${FHIRUSER_SCOPE}|g" "${WSO2_OH_APIM_HOME}"/repository/conf/identity/oidc-scope-config.xml
fi

# adds launch/patient
LAUNCH_PATIENT_SCOPE="<Scopes>\n\t<Scope id=\"launch/patient\">\n\t\t<Claim>patientId</Claim>\n\t</Scope>\n"
if grep -Fq '<Scope id="launch/patient">' "${WSO2_OH_APIM_HOME}"/repository/conf/identity/oidc-scope-config.xml
then
    # do nothing
    echo -e "[WARN] launch/patient scope configuration already exist"
else
    sed -i -e "s|<Scopes>|${LAUNCH_PATIENT_SCOPE}|g" "${WSO2_OH_APIM_HOME}"/repository/conf/identity/oidc-scope-config.xml
fi

# adds offline_access
OFFLINE_ACCESS_SCOPE="<Scopes>\n\t<Scope id=\"offline_access\">\n\t\t<Claim>patientId</Claim>\n\t</Scope>\n"
if grep -Fq '<Scope id="offline_access">' "${WSO2_OH_APIM_HOME}"/repository/conf/identity/oidc-scope-config.xml
then
    # do nothing
    echo -e "[WARN] offline_access scope configuration already exist"
else
    sed -i -e "s|<Scopes>|${OFFLINE_ACCESS_SCOPE}|g" "${WSO2_OH_APIM_HOME}"/repository/conf/identity/oidc-scope-config.xml
fi

echo -e "[INFO] WSO2 Open Healthcare APIM Accelerator is successfully applied"

echo -e "$(date)" - "$USER" - "${WSO2_OH_ACCELERATOR_VERSION}" | tee -a "${WSO2_OH_ACCELERATOR_AUDIT}"/merge_audit.log >/dev/null

echo -e "[INFO] Done"
