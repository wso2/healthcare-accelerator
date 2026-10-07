# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WSO2 Healthcare Accelerator provides core functionality to streamline healthcare integration by enabling rapid development and deployment of healthcare applications with support for standards like FHIR and HL7, SMART on FHIR, and OAuth 2.0.

The project consists of two main accelerator distributions:
- **API Manager (APIM) Accelerator**: Healthcare-specific API management features
- **Identity Server (IS) Accelerator**: Healthcare-specific identity and access management

## Build Commands

### Full Build
```bash
mvn clean install
```

### Build Individual Accelerators
```bash
# APIM Accelerator only
cd product-accelerators/apim
mvn clean install

# IS Accelerator only
cd product-accelerators/is
mvn clean install

# APIM Distribution
cd distribution/apim-accelerator
mvn clean install

# IS Distribution
cd distribution/is-accelerator
mvn clean install
```

### Run Tests
```bash
# All tests
mvn test

# Single test
mvn test -Dtest=PrivateKeyJWTClientAuthenticatorTest

# Skip tests during build
mvn clean install -DskipTests
```

### View Dependency Tree
```bash
mvn dependency:tree
```

## Architecture Overview

### Module Structure

The project follows a multi-module Maven structure:

```
healthcare-accelerator/
├── product-accelerators/          # Core accelerator components
│   ├── apim/                      # API Manager accelerator
│   │   ├── components/            # OSGi components for APIM
│   │   └── apps/                  # Web applications (portals)
│   └── is/                        # Identity Server accelerator
│       └── components/            # OSGi components for IS
└── distribution/                  # Distribution packages
    ├── apim-accelerator/          # APIM distribution artifacts
    └── is-accelerator/            # IS distribution artifacts
```

### APIM Accelerator Components

Located in `product-accelerators/apim/components/`:

1. **org.wso2.healthcare.apim.core**: Core utilities, configuration management, caching, data sources, and common APIs
   - Central configuration via TOML files
   - HTTP utilities, security utilities, database utilities
   - Email notification system
   - Reference holder for OSGi services

2. **org.wso2.healthcare.apim.claim.mgt**: JWT claim management and user claim issuing
   - Custom claim providers for JWT generation
   - User claim resolution from user stores

3. **org.wso2.healthcare.apim.clientauth.jwt**: Private Key JWT client authentication (RFC 7523)
   - JWT validator with JWKS support
   - JWT cache and storage management
   - Client authentication for OAuth2 token endpoints

4. **org.wso2.healthcare.apim.conformance**: FHIR CapabilityStatement auto-generation
   - OpenAPI/Swagger parsing (OAS2 and OAS3)
   - Metadata endpoint generation (`/r4/metadata`)
   - SMART configuration endpoint (`/.well-known/smart-configuration`)

5. **org.wso2.healthcare.apim.scopemgt**: FHIR scope management and validation
   - SMART on FHIR scope handling (e.g., `patient/*.read`, `launch/patient`)
   - Scope-to-role mapping
   - Custom key validation handler for FHIR scopes

6. **org.wso2.healthcare.apim.tokenmgt**: OAuth2 token management
   - Custom authorization code grant handler
   - SMART launch context injection

7. **org.wso2.healthcare.apim.gateway.security.jwt.generator**: Gateway JWT generation with healthcare claims

8. **org.wso2.healthcare.apim.multitenancy**: Multi-tenant conformance API builders
   - Metadata API builder
   - SMART config API builder
   - System API handling

9. **org.wso2.healthcare.apim.backendauth**: Backend authentication mediators
   - Client credentials authenticator
   - Private Key JWT backend authenticator
   - Token management and caching

10. **org.wso2.healthcare.apim.workflow.extensions**: Workflow executors for user signup and application creation approvals

### IS Accelerator Components

Located in `product-accelerators/is/components/`:

1. **org.wso2.healthcare.is.smart.auth**: SMART on FHIR authentication for IS 7.2.0
   - Token response handler for SMART launch contexts
   - User claim resolver for patient/practitioner IDs
   - Supports `launch/patient` and `launch/practitioner` scopes

2. **org.wso2.healthcare.is.tokenmgt**: Custom OAuth2 grant handlers for IS
   - Authorization code grant handler with patient context injection
   - Migrated from APIM to IS 7.2.0 compatibility

### Web Applications

Located in `product-accelerators/apim/apps/`:
- **authentication-portal**: Custom authentication endpoint
- **recovery-portal**: Account recovery functionality
- **identity-shared**: Shared identity components

## Key Technologies

- **OSGi/Apache Felix**: Component lifecycle management
- **Maven**: Build and dependency management
- **HAPI FHIR 4.1.0**: FHIR resource handling
- **WSO2 Carbon**: Platform framework (API Manager 9.29.120, Identity Framework 7.2.34)
- **Nimbus JOSE+JWT**: JWT processing
- **Swagger/OpenAPI**: API definition parsing
- **Apache Synapse**: Mediation and message routing

## Development Patterns

### OSGi Service Components

Components use Apache Felix DS annotations:
```java
@Component(
    name = "healthcare.component",
    immediate = true
)
public class HealthcareComponent {
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected void setRealmService(RealmService realmService) {
        // Service binding
    }
}
```

### Configuration Management

TOML-based configuration loaded via `org.wso2.healthcare.apim.core.config.*`:
- All healthcare configurations are under `[healthcare]` namespace
- Configuration classes in `org.wso2.healthcare.apim.core.config` package

### Data Access

- Use `org.wso2.healthcare.apim.core.datasources.*` for database operations
- SQL constants in `org.wso2.healthcare.apim.core.dao.SQLConstants`
- Database utilities in `org.wso2.healthcare.apim.core.utils.DBUtils`

### Error Handling

- Custom exceptions: `OpenHealthcareException`, `OpenHealthcareRuntimeException`, `OpenHealthcarePermissionException`
- Error utilities in `org.wso2.healthcare.apim.core.utils.ErrorUtil`

## SMART on FHIR Implementation

### Scope Handling

FHIR scopes follow the pattern: `{compartment}/{resourceType}.{permission}`
- Examples: `patient/*.read`, `user/Observation.write`, `launch/patient`
- Scope validation in `org.wso2.healthcare.apim.scopemgt.handlers.OpenHealthcareExtendedKeyValidationHandler`

### Launch Context

- `launch/patient`: Adds `patient` parameter to token response
- `launch/practitioner`: Adds `practitioner` parameter to token response
- Patient/practitioner IDs resolved from user claims

### Token Response Format

When SMART launch scopes are present, token responses include context:
```json
{
  "access_token": "...",
  "patient": "patient-123",
  "scope": "launch/patient patient/*.read"
}
```

## Testing

### Unit Tests

- Use PowerMock and Mockito for mocking
- H2 database for DAO testing
- Test utilities in packages ending with `.util` (e.g., `JWTTestUtil`)

### Key Test Classes

- `PrivateKeyJWTClientAuthenticatorTest`: JWT authentication flow
- `JWTValidatorTest`: JWT validation logic
- `JWTStorageManagerTest`: JWT persistence
- `ErrorUtilTest`: Error handling utilities

## Important Notes for Development

### APIM Dependencies

The codebase heavily integrates with WSO2 APIM classes. See the comprehensive dependency table in `product-accelerators/apim/README.md` for:
- API Management operations
- OAuth & token management
- User & identity management
- Registry & configuration
- Notifications & events

### IS vs APIM Components

- IS components (`org.wso2.healthcare.is.*`) are for Identity Server 7.2.0+
- APIM components (`org.wso2.healthcare.apim.*`) are for API Manager 9.29.120
- IS components do NOT depend on APIM components
- APIM token management was migrated and simplified for IS

### Private Key JWT Authentication

Implementation follows RFC 7523:
- JWT cache with configurable expiry
- Database-backed JWT storage for replay prevention
- Maximum JWT lifetime: 300 seconds (configurable)
- Supports both client certificate and JWKS-based validation

### Code Style

- Apache 2.0 license header required in all source files
- Checkstyle validation enabled (WSO2 code quality standards)
- Java 8 compatibility required

## Distribution and Deployment

### APIM Accelerator

1. Build: `cd distribution/apim-accelerator && mvn clean install`
2. Extract generated ZIP from `target/`
3. Run `merge.sh` script to integrate with WSO2 APIM
4. Creates audit logs in `hc-accelerator/merge_audit.log`
5. Backs up original files in `hc-accelerator/backup/`

### IS Accelerator

1. Build: `cd distribution/is-accelerator && mvn clean install`
2. Extract generated ZIP from `target/`
3. Copy JARs from `dropins/` to `<IS_HOME>/repository/components/dropins/`
4. Configure claims and grant handlers in IS deployment.toml
5. Restart Identity Server

## Git Workflow

- Current branch: `fixes/smartonfhir`
- Main branch: `main`
- Recent commits indicate work on SMART on FHIR features and code grant handlers
