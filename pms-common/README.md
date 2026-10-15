# pms-common

Shared module containing security, JWT token handling, and common audit utilities used by PMS microservices.

## Security package (`com.pms.security.jwt`)
- `JwtProperties` — configuration properties (`security.jwt.*`)
- `JwtTokenService` — generates access and refresh tokens
- `AccessTokenVerifier` — validates access and refresh tokens
- `JwtAuthenticationFilter` — stateless JWT filter registered by `SecurityAutoConfiguration`
- `RequestUserContext` — ThreadLocal holder for the current request username
- `CurrentUserProvider` / `RequestCurrentUserProvider` — injectable current-user abstraction
- `SecurityAutoConfiguration` — auto-configures all security beans when `security.jwt.secret` is set

## Audit package (`com.pms.common.audit`)
- `BaseEntity` — JPA audited base class (`createdBy`, `updatedBy` as UUID)
- `JpaAuditingConfig` — enables JPA auditing
- `CurrentUserProvider` — interface for audit current-user lookup
- `CurrentUserAuditorAware` — bridges `CurrentUserProvider` into Spring Data

## Build locally
```powershell
mvn -f .\pms-common\pom.xml clean install
```
