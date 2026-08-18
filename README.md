# Folio and Billing Service

Spring Boot service implementing the following APIs:
- /api/v1/billingFolio/getFolioBilling
- /api/v1/billingFolio/getBillingDetails
- /api/v1/billingFolio/folioDashboard
- /api/v1/billingFolio/addCharge
- /api/v1/billingFolio/adjustCharge
- /api/v1/billingFolio/allocatePayment
- /api/v1/billingFolio/paymentAllocationHistory
- /api/v1/billingFolio/generateDocument
- /api/v1/billingFolio/documents/{documentId}/download
- /api/v1/billingFolio/documents/{documentId}/print
- /api/v1/billingFolio/documentAuditHistory

## Endpoints

### 1) Get Folio Billing
Path: /api/v1/billingFolio/getFolioBilling

Query parameters (optional):
- roomNumber
- guestName
- company
- confirmationNumber
- checkInDate (ISO date: yyyy-MM-dd)
- checkOutDate (ISO date: yyyy-MM-dd)

Response fields per row:
- tier
- lastName
- firstName
- room
- guest
- stayStatus
- checkIn
- checkOut
- nights
- houseKeeping
- roomType
- confirmationNo

### 2) Get Billing Details
Path: /api/v1/billingFolio/getBillingDetails

Query parameters (optional):
- confirmationNo
- roomNo
- guestName

Response fields:
- totalCharges
- totalPayment
- balance
- guestName
- guest1
- guest2
- confirmationNo
- adults
- children
- company
- bookingSource
- ratePlan
- reservationStatus
- folioStatus
- roomNo
- roomType
- checkInDate
- checkOutDate
- nights
- comments

### 3) Folio Dashboard
Path: /api/v1/billingFolio/folioDashboard

Query parameters (optional):
- confirmationNo

Response:
- folioADashboard: list of transaction rows
  - date
  - referenceNumber
  - transactionType
  - category
  - description
  - charges
  - tax
  - credit
  - userId
  - postedAt
  - originalReferenceNumber
  - adjustmentReason
- guestDetails: list of guests
  - guestName
  - guestAge
  - guestPhoneNumber
  - guestEmailId
  - guestAddress
  - dueAmount

### 4) Add Folio Charge
Path: /api/v1/billingFolio/addCharge
Method: POST

Request body:
- confirmationNo (required)
- roomNo (optional)
- guestName (optional)
- category (required, for example ROOM, MINIBAR, LAUNDRY, PARKING)
- description (required)
- amount (required, must be > 0)
- Tax is calculated automatically from active Property Service tax rules for the request's `X-Property-Id`.
- postingDate (optional, defaults to current date)
- userId (optional)

Response fields:
- confirmationNo
- referenceNumber
- transactionType
- category
- description
- amount
- tax
- taxDetails (one entry per applied property tax rule: taxName, rate, amount)
- postingDate
- totalCharges
- totalPayment
- balance

### 5) Adjust Posted Charge
Path: /api/v1/billingFolio/adjustCharge
Method: POST

Request body:
- confirmationNo (required)
- originalReferenceNumber (required, the transaction being adjusted)
- adjustmentType (required, INCREASE or DECREASE)
- amount (required, must be > 0)
- Tax is recalculated from the tax snapshot applied to the original charge.
- reason (required)
- userId (optional)

Response fields:
- confirmationNo
- originalReferenceNumber
- adjustmentReferenceNumber
- adjustmentType
- category
- reason
- amount
- tax
- taxDetails (one entry per applied property tax rule: taxName, rate, amount)
- postingDate
- postedAt
- userId
- totalCharges
- totalPayment
- balance

### 6) Allocate Payment To One Or Multiple Folios
Path: /api/v1/billingFolio/allocatePayment
Method: POST

Request body:
- paymentAmount (required, must be > 0)
- paymentReference (optional, auto-generated when not sent)
- paymentMethod (optional, default Card)
- allocationDate (optional, defaults to current date)
- allocations (required, one or more items)
  - confirmationNo (required)
  - amount (required, must be > 0)
- userId (optional)
- note (optional)

Response fields:
- paymentReference
- paymentAmount
- totalAllocatedAmount
- unallocatedAmount
- paymentMethod
- allocationDate
- allocatedAt
- userId
- allocations
  - confirmationNo
  - transactionReferenceNumber
  - allocatedAmount
  - balanceBeforeAllocation
  - balanceAfterAllocation

Behavior:
- Supports single-folio and multi-folio allocation in one request.
- Supports partial allocation when totalAllocatedAmount is less than paymentAmount.
- Prevents over-allocation:
  - totalAllocatedAmount cannot exceed paymentAmount.
  - each folio allocation cannot exceed that folio outstanding balance.
- Each successful allocation posts payment credits to folio transactions and updates folio balances immediately.

### 7) Payment Allocation History
Path: /api/v1/billingFolio/paymentAllocationHistory
Method: GET

Query parameters (optional):
- confirmationNo
- paymentReference

Response: list of allocation history entries containing:
- paymentReference
- confirmationNo
- paymentAmount
- totalAllocatedAmount
- allocatedAmount
- unallocatedAmount
- paymentMethod
- allocationDate
- allocatedAt
- userId
- note
- balanceAfterAllocation

### 8) Generate Folio Receipt Or Invoice
Path: /api/v1/billingFolio/generateDocument
Method: POST

Request body:
- confirmationNo (required)
- documentType (required, INVOICE or RECEIPT)
- userId (optional)

Response fields:
- documentId
- confirmationNo
- documentType
- fileName
- contentType
- generatedAt
- generatedBy
- totalChargeAmount
- totalTaxAmount
- totalPaymentAmount
- latestBalance
- downloadPath
- printPath

Behavior:
- Generated document includes guest details and reservation details.
- Generated document includes transaction rows with charge, tax, and payment details.
- Generated document includes a tax breakdown grouped by tax type.
- Generated document totals are calculated from the latest folio transactions.
- latestBalance in response reflects current folio balance at generation time.

### 9) Download Generated Document
Path: /api/v1/billingFolio/documents/{documentId}/download
Method: GET

Behavior:
- Returns the generated HTML document as downloadable content with attachment headers.

### 10) Print Generated Document
Path: /api/v1/billingFolio/documents/{documentId}/print
Method: GET

Behavior:
- Returns the generated HTML document inline for browser print workflow.
- Document contains a print button (`window.print()`) and print-friendly CSS.

### 11) Document Audit History
Path: /api/v1/billingFolio/documentAuditHistory
Method: GET

Query parameters (optional):
- confirmationNo
- documentType (INVOICE or RECEIPT)

Response: list of document audit entries containing:
- documentId
- confirmationNo
- documentType
- fileName
- generatedAt
- generatedBy
- totalChargeAmount
- totalTaxAmount
- totalPaymentAmount
- latestBalance

## Integration points

The service uses live Reservation Service and Property Service integrations.

Note:
- There is no separate billing-ledger microservice in this requirement.
- Totals and transactions are created from charges, adjustments, and payments posted through this service.
- Folio records are auto-created/maintained in service memory when reservation data is fetched.
- Room and ancillary charges can be posted to folio using addCharge API.
- Posted charges can be increased or decreased using adjustCharge API.
- Adjustment reason is mandatory and retained on the transaction row as adjustmentReason.
- Adjustment audit includes userId, postedAt timestamp, adjustment reference, and originalReferenceNumber link.
- Payments/credits from folio transactions automatically update folio payment totals and outstanding balance.
- New charges update folio balance immediately after posting.
- `ROOM` charges are not taxed by ADD_ON rules. All other charge categories use active `ADD_ON`, `PERCENTAGE`, `EXCLUSIVE` rules from Property Service for the request's `X-Property-Id`.
- `X-Property-Id` is required when posting a non-ROOM charge so the applicable property tax rules can be resolved.
- Charge adjustments update folio balance immediately after posting.
- Payment allocation supports one or multiple folios with partial allocation support.
- Over-allocation is blocked both at payment-level and folio-level.
- Payment allocation history is maintained in service memory and exposed via paymentAllocationHistory API.
- Folio invoice/receipt generation is supported and uses latest folio financial snapshot.
- Generated documents can be downloaded or opened for printing.
- Generated document audit history is maintained in service memory and exposed via documentAuditHistory API.
- Reservation data is retrieved from the live reservation service (`integration.reservation-service.base-url`).
- Live guest-listing calls use propertyId in this order:
  - `X-Property-Id` request header (preferred)
  - `integration.reservation-service.property-id` config (fallback)

## Security

This resource service now uses stateless JWT Bearer authentication aligned with your auth-service pattern:
- Access tokens only (`typ=access` claim required)
- Shared secret signature verification
- Roles claim mapped to Spring authorities with `ROLE_` prefix
- Non-public endpoints protected with `ADMIN` role
- Missing or invalid token on protected endpoints returns `401`

Main classes:
- AccessTokenVerifier
- JwtAccessTokenValidator
- AuthFilter
- SecurityConfig

Configure shared secret in `application.yml`:
- `security.jwt.secret`
- `security.jwt.secret-base64-encoded`
- `security.jwt.public-paths`

Expected JWT claims:
- `sub` = username
- `roles` = string list or comma-separated roles
- `typ` = `access`

## Build and run

Build:
- mvn -DskipTests compile

Run:
- mvn spring-boot:run
