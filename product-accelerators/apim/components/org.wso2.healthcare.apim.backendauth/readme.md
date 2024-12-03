# Backend Authentication for WSO2 API Manager

This module provides a streamlined solution for integrating an external authentication server to securely obtain and 
manage access tokens for backend service requests. By default, WSO2 API Manager's gateway forward the end-user's access token 
from the invoking client to the backend API. 
<br>This module replaces that default behavior by fetching a new access token 
from an external OAuth 2.0-compliant authentication server and replacing the `Authorization` header with the 
retrieved token.

## Configuration Model

```toml
[healthcare.backend.auth]
token_endpoint = "https://localhost:9443/oauth2/token"
client_id = "client_id"
client_secret = "client_secret"
private_key_alias = "wso2carbon"
is_ssl_enabled = true
```

## Key Features

- **External Auth Server Integration**: Connect to an external authentication server that supports OAuth 2.0 to 
retrieve tokens for backend services.
- **Grant Type Support**: Supports the following OAuth 2.0 grant types:
    - JWT Bearer Grant
    - Client Credentials Grant (In-Progress. Will be available in the next release)
- **Dynamic Header Replacement**: Automatically replaces the existing `Authorization` header in API requests with the 
access token retrieved from the configured auth server.

## Use of Private-Public key pair for PKJWT Auth flow
1. Generate an RSA key pair. Recommended signature algorithm is `RS256`.
2. Upload the private key to the WSO2 API Manager's Key Store. The alias of the uploaded key should be 
provided in the configuration(`private_key_alias`).
3. Configure the public key in the external authentication server.
4. If you're enabling SSL, provide the public key of the external authentication server in the 
WSO2 API Manager's truststore(`client-truststore.jks`).


## Important Notes
### Convert a Private Key from `.pem` to `PKCS#12` Format
```bash 
openssl pkcs12 -export \                                           
  -in <cert_for_sign> \  
  -inkey <privatekey_in_pem_format> \    
  -out <out_file_name>.p12 \
  -name <alias> \   
  -passout <password>
``` 

### Import a Private Key in `PKCS#12` format to a `JKS` Key Store
```bash
keytool -importkeystore \ 
  -srckeystore <p12_file_path> \ 
  -srcstoretype PKCS12 \ 
  -destkeystore <jks_file_path> \ 
  -deststoretype JKS \ 
  -srcstorepass <p12_file_password> \ 
  -deststorepass <jks_password> \
  -srcalias <alias_used_in_p12> \
  -destalias <alias_to_be_used_in_JKS>
```

