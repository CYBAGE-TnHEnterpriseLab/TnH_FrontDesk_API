# pms-security-common

Shared JWT validation module used by PMS microservices.

## Contains
- `com.pms.security.jwt.AccessTokenVerifier`
  - Validates signature
  - Enforces `typ=access`
  - Extracts `username` and `roles`

## Build locally
```powershell
mvn -f .\pms-security-common\pom.xml clean install
```

