3# Property Service - Step-by-Step Application Flow

This document explains what the current application does, end-to-end, in practical sequence.

## 1) Application startup

1. `PropertyServiceApplication` boots Spring Boot.
2. Configuration is loaded from `src/main/resources/application.properties`.
3. H2 in-memory DB is started in PostgreSQL compatibility mode.
4. Flyway runs migrations (`V1` to `V5`) and creates/updates schema.
5. JPA starts with `ddl-auto=validate`, so entity mappings must match migration schema.
6. REST endpoints become available.

## 2) Core API surface

All wizard APIs are exposed via `DraftController` at:

- `POST /api/property/drafts` -> create draft
- `PUT /api/property/drafts/{draftId}` -> save/update draft
- `GET /api/property/drafts/{draftId}` -> fetch draft
- `POST /api/property/drafts/{draftId}/publish` -> publish draft

Responses are wrapped by `ApiResponse`.

## 3) Draft creation flow (`POST /api/property/drafts`)

1. Controller validates request (`CreateDraftRequest`).
2. `DraftFacade` calls `DraftService.createDraft`.
3. Service creates a `PropertyDraftEntity` with:
   - `status = DRAFT`
   - `lifecycleState = DRAFT`
   - `currentStep` (default `PROPERTY_DETAILS` if missing)
   - `completedSteps` (comma-separated string)
   - full `wizardData` JSON serialized as string
4. Entity is saved in `property_draft`.
5. `DraftMapper` returns API response with parsed JSON and metadata.

## 4) Draft save flow (`PUT /api/property/drafts/{id}`)

1. Controller validates request (`SaveDraftRequest`).
2. `DraftService.saveDraft` loads draft by ID.
3. If draft is already published, save is blocked.
4. If `expectedVersion` is provided, optimistic version check is enforced.
5. Draft fields are updated:
   - latest `wizardData`
   - `currentStep` and `completedSteps`
   - `lifecycleState` moves from `DRAFT` to `CONFIGURED` on first successful update
6. Updated draft is saved and returned.

## 5) Draft fetch flow (`GET /api/property/drafts/{id}`)

1. Service reads `property_draft` by ID.
2. If missing, `NotFoundException` is thrown.
3. Mapper parses JSON and returns full draft state for wizard resume.

## 6) Publish flow (`POST /api/property/drafts/{id}/publish`)

1. Service loads draft.
2. If draft already published:
   - returns existing `publishedPropertyId` (idempotent-ish behavior for repeated publish calls)
   - throws error only if published status exists but property id is missing
3. `PublishMapper` converts draft JSON into normalized module entities.
4. `PublishValidator` enforces business rules before DB write.
5. Inside one transaction, service saves:
   - property profile and timings
   - content (`special_requests`, `amenities`)
   - room module (`room_type`, `floor_configuration`, auto/manual `inventory_room`)
   - finance (`chart_of_account`, `revenue_mapping`)
   - payments (`payment_method`, `payment_gateway_config`)
   - taxes (`tax_rule`)
6. Draft is marked published (`status = PUBLISHED`, `lifecycleState = ACTIVE`, link to `publishedPropertyId`).
7. Publish response returns draft id + published property id.

## 7) Validation rules applied at publish time

`PublishValidator` currently checks:

- Property details: required fields, address length, HH:mm time format, check-in/out mismatch.
- Room configuration: at least one room type + floor, unique room type names, master mapping integrity, floor-to-room-type validity, positive room count, no duplicate explicit room numbers.
- Finance: chart of accounts required, revenue mappings required, mapping must reference existing ledger code.
- Payments: at least one payment method; gateway fields required.
- Taxes: at least one tax rule; required tax fields; tax value must be > 0.

## 8) Auto room number behavior

During publish mapping:

- If a floor has manual `roomNumbers`, those are used.
- Otherwise, numbers are generated from `startNumber` to `startNumber + roomCount - 1`.

Uniqueness is protected by DB constraint `(property_id, room_number)`.

## 9) Error handling behavior

`GlobalExceptionHandler` maps exceptions to HTTP responses:

- `NotFoundException` -> `404`
- `BadRequestException` -> `400`
- Bean validation errors -> `400` with first field error
- Unhandled errors -> `500`

## 10) Database evolution overview

- `V1`: `property_draft`
- `V2`: base normalized tables (`property`, `room`, `tax`, `finance`, `revenue`)
- `V3`: indexes
- `V4`: draft lifecycle fields + richer property operational fields
- `V5`: wizard publish domain tables for content/room/finance/payment/tax

## 11) Security note in current implementation

Payment gateway `apiKey` and `secret` are encoded using Base64 in `PublishMapper` as a placeholder.

- This is **not** strong encryption.
- For production, replace with KMS/keystore-backed encryption + secret rotation.

## 12) Current test coverage

- `PropertyServiceApplicationTests`: verifies context boot + migrations + repositories wiring.
- `PublishMapperTest`: verifies JSON-to-normalized mapping for the expanded wizard structure.

## 13) Current lifecycle state usage

Defined states: `DRAFT`, `CONFIGURED`, `VALIDATED`, `ACTIVE`.

As implemented now:

- create -> `DRAFT`
- save -> moves to `CONFIGURED` (from `DRAFT`)
- publish -> `ACTIVE`
- `VALIDATED` is defined but not yet assigned by a separate validation stage.

