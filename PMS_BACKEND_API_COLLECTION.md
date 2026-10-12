# PMS Backend API Collection

This collection is derived from the Spring controllers and DTOs currently present in
the repository. It documents application APIs only; `pms-common`, `pms-security-common`
and `API-Gateway` are not included as API repositories because they do not expose
controller endpoints. `Policy` is documented as its own backend repository.

## Conventions

- Unless noted otherwise, protected endpoints use `Authorization: Bearer <JWT>`.
- `pms-auth` authentication endpoints are explicitly public.
- `ApiResponse` wrappers vary by repository. Reservation/auth/dashboard wrappers use
  `success`, `message`, and `data` (some reservation check-in responses also include
  `errors` and `timestamp`). Property uses `success`, `message`, and `data`. Policy
  uses `status`, `message`, and `data`.
- DTO examples are representative and intentionally omit nonessential nested data.
- Date values use ISO `YYYY-MM-DD`; timestamps use the representation emitted by the
  DTO.

---

## 1. pms-auth

Authentication endpoints are public. Successful responses are wrapped as
`{"success":true,"message":"...","data":{...}}`.

### 1.1 Register

**Endpoint:** `POST /api/v1/auth/register`

**Purpose:** Create a user account and issue access and refresh tokens.

**Request body:**

```json
{"username":"frontdesk","email":"frontdesk@example.com","password":"StrongPassword1!"}
```

Required: `username`, `email`, `password`. Password must satisfy the configured
password policy. Registration currently assigns the `ADMIN` role.

**Response: `200 OK`**

```json
{"success":true,"message":"User registered successfully","data":{
  "accessToken":"<jwt>","refreshToken":"<jwt>","tokenType":"Bearer",
  "accessTokenExpiresInSeconds":900,"roles":["ADMIN"]
}}
```

**Errors:** `400` duplicate username/email or business failure; `422` validation
failure; `500` unexpected failure.

### 1.2 Login

**Endpoint:** `POST /api/v1/auth/login`

**Purpose:** Authenticate an enabled user.

**Request body:**

```json
{"username":"frontdesk","password":"StrongPassword1!"}
```

Required: `username`, `password`.

**Response:** Same token structure as Register, with message `Login successful`.

**Errors:** `400` invalid credentials or non-admin account; `422` validation
failure; `500` unexpected failure.

### 1.3 Refresh Access Token

**Endpoint:** `POST /api/v1/auth/refresh`

**Request body:** `{"refreshToken":"<refresh-jwt>"}`

Required: `refreshToken`. Refresh-token rotation is performed by the service.

**Response:** `200 OK`, token wrapper containing a new access and refresh token.

**Errors:** `400` invalid, revoked, expired, or mismatched refresh token;
`422` validation failure; `500` unexpected failure.

### 1.4 Logout

**Endpoint:** `POST /api/v1/auth/logout`

**Request body:** `{"refreshToken":"<refresh-jwt>"}`

Required: `refreshToken`.

**Response:** `200 OK`

```json
{"success":true,"message":"Logout successful","data":null}
```

**Errors:** `400` invalid refresh token; `422` validation failure.

---

## 2. pms-guest

The service uses property-scoped guest profiles. Its controller returns guest DTOs
directly rather than the common `ApiResponse` wrapper. `/api/v1/guests/**`
requires `ADMIN`; the service security configuration does not define a separate
front-desk role.

### 2.1 Lookup Guest Profile

**Endpoint:** `GET /api/v1/guests/lookup`

**Query parameters:**

- `propertyId` — required.
- At least one of `firstName`, `lastName`, `phoneNumber`, `personalEmail`,
  `loyaltyNumber` — required.

**Response: `200 OK`**

```json
{"id":123,"propertyId":"P001","firstName":"John","lastName":"Doe",
 "phoneNumber":"9999999999","personalEmail":"john@example.com"}
```

**Errors:** `400` missing property or lookup identifier; `404` no match;
`409` multiple matches.

### 2.2 Create Guest Profile

**Endpoint:** `POST /api/v1/guests`

**Request body:** `GuestProfileCreateRequest`

```json
{"propertyId":"P001","salutation":"Mr","firstName":"John","lastName":"Doe",
 "personalEmail":"john@example.com","phoneNumber":"9999999999",
 "country":"India","vipStatus":false,"loyaltyMembershipNumber":"L100"}
```

Important fields include `propertyId`, `firstName`, `lastName`, contact fields,
address fields, identity fields, `vipStatus`, `loyaltyMembershipNumber`, and
`loyaltyTier`. Bean validation applies to the request DTO.

**Response: `201 Created`** — created `GuestProfileResponse`.

**Errors:** `400` invalid request; `409` duplicate/conflicting profile.

### 2.3 Get Guest Profile

**Endpoint:** `GET /api/v1/guests/{id}`

**Path parameters:** `id` — guest profile database ID.

**Query parameters:** `propertyId` — required property scope.

**Response:** `200 OK`, `GuestProfileResponse`.

**Errors:** `400` missing property ID; `404` profile not found.

### 2.4 Update Guest Profile

**Endpoint:** `PUT /api/v1/guests/{id}`

**Path parameters:** `id`.

**Query parameters:** `propertyId` — required.

**Request body:** `GuestProfileUpdateRequest`; contains editable name, contact,
address, identity, loyalty, VIP, and date-of-birth fields.

**Response:** `200 OK`, updated `GuestProfileResponse`.

**Errors:** `400` invalid request; `404` profile not found; `409` conflict.

### 2.5 Delete Guest Profile

**Endpoint:** `DELETE /api/v1/guests/{id}`

**Path parameters:** `id`.

**Query parameters:** `propertyId` — required.

**Response:** `204 No Content`.

**Errors:** `400` missing property ID; `404` profile not found; `409` profile has
reservation history and cannot be deleted.

### 2.6 Search Guest Profiles

**Endpoint:** `GET /api/v1/guests/search`

**Query parameters:** required `propertyId`; optional `firstName`, `lastName`,
`phoneNumber`, `personalEmail`, `officialEmail`, `loyaltyNumber`.

**Response:** `200 OK`, array of `GuestProfileResponse`.

**Errors:** `400` missing property ID or invalid filter.

### 2.7 Get Guest Details

**Endpoint:** `GET /api/v1/guests/details`

**Query parameters:** required `propertyId`; exactly one of `phoneNumber`,
`email`, `bookingId`, `confirmationNumber`.

**Response: `200 OK`**

```json
{"propertyId":"P001","guestDetails":[
  {"guestProfileId":123,"bookingId":456,"confirmationNumber":"CN001",
   "firstName":"John","lastName":"Doe"}
]}
```

**Errors:** `400` missing property or not exactly one criterion; `502` reservation
service unavailable.

---

## 3. pms-reservation

Most endpoints require `ADMIN` under the module security configuration. Responses
from reservation controllers generally use:
`{"success":true,"message":"...","data":...}`.

### 3.1 List Payment Modes

**Endpoint:** `GET /api/v1/reservations/payment-modes`

**Request:** No parameters or body.

**Response:** `200 OK`, wrapped array of supported payment-mode strings.

### 3.2 List Identity Types

**Endpoint:** `GET /api/v1/reservations/id-types`

**Request:** None.

**Response:** `200 OK`, wrapped array of supported identity-type strings.

### 3.3 Create Reservation Booking

**Endpoint:** `POST /api/v1/reservations/bookings`

**Request body:** `ReservationBookingRequestDto` (Create validation group).
Important fields:

```json
{"propertyId":"P001","firstName":"John","lastName":"Doe",
 "phoneNumber":"9999999999","arrivalDate":"2026-10-10",
 "departureDate":"2026-10-12","adultCount":2,"childCount":0,
 "roomType":"Deluxe","numberOfRooms":1,"rate":2500,
 "payment":"CASH","paymentType":"FULL"}
```

The DTO also supports `guests`, `guestNames`, `room`, `rate`, tax/discount,
company/source/agent, `eta`, `checkOutTime`, `dnm`, `noPost`, balance, and
special-request fields. Dates and counts are validated by the DTO/service.

**Response: `201 Created`**

```json
{"success":true,"message":"Reservation confirmed successfully","data":{
  "confirmationNumber":"CN001","bookingId":456,"status":"CONFIRMED"
}}
```

**Errors:** `400` business/availability/payment failure; `422` validation failure;
`409` conflicting reservation; `500` unexpected failure.

### 3.4 Get Booking Details

**Endpoint:** `GET /api/v1/reservations/bookings/{confirmationNumber}`

**Path parameters:** `confirmationNumber`.

**Response:** `200 OK`, wrapped `ReservationViewResponseDto`, containing the
confirmation and room-booking/guest/stay information.

**Errors:** `404` reservation not found; `400` invalid confirmation.

### 3.5 Update Guest Balance

**Endpoint:** `PATCH /api/v1/reservations/bookings/{confirmationNumber}/guest-balance`

**Path parameters:** `confirmationNumber`.

**Query parameters:** optional `bookingId`; required `guestBalance`.

**Response:** `204 No Content`.

**Errors:** `400` invalid balance; `404` booking not found.

### 3.6 Search Booking

**Endpoint:** `GET /api/v1/reservations/bookings/search`

**Query parameters:** optional `confirmationNumber`, `bookingId`, `propertyId`,
`phoneNumber`, `email`. The service resolves the matching reservation.

**Response:** `200 OK`, wrapped `ReservationViewResponseDto`.

**Errors:** `400` invalid/insufficient search; `404` no matching booking.

### 3.7 Get One Room Booking

**Endpoint:** `GET /api/v1/reservations/bookings/{confirmationNumber}/rooms/{bookingId}`

**Path parameters:** `confirmationNumber`, `bookingId`.

**Response:** `200 OK`, wrapped `ReservationViewResponseDto`.

**Errors:** `404` booking or room booking not found.

### 3.8 Update Reservation

**Endpoint:** `PATCH /api/v1/reservations/bookings/{confirmationNumber}`

**Path parameters:** `confirmationNumber`.

**Request body:** `ReservationBookingRequestDto`; fields are applied to the
confirmation-level reservation.

**Response:** `200 OK`, wrapped `ReservationViewResponseDto`.

**Errors:** `400` invalid update; `404` not found; `409` state/availability conflict.

### 3.9 Update One Room Booking

**Endpoint:** `PATCH /api/v1/reservations/bookings/{confirmationNumber}/rooms/{bookingId}`

**Path parameters:** `confirmationNumber`, `bookingId`.

**Request body:** `ReservationBookingRequestDto`.

**Response:** `200 OK`, wrapped `ReservationViewResponseDto`.

**Errors:** `400`, `404`, or `409` according to update validation/state.

### 3.10 List Reservations

**Endpoint:** `GET /api/v1/reservations/bookings`

**Request:** No parameters or body.

**Response:** `200 OK`, wrapped array of `ReservationBookingResponseDto`.

### 3.11 Synchronize Housekeeping Statuses

**Endpoint:** `POST /api/v1/reservations/housekeeping/sync`

**Request:** No parameters or body.

**Response: `200 OK`**

```json
{"success":true,"message":"Housekeeping synchronization completed",
 "data":{"processed":10,"updated":8,"skipped":1,"failed":1}}
```

**Errors:** `502`/`500` when downstream synchronization fails.

### 3.12 List Payment Types

**Endpoint:** `GET /api/v1/reservations/payment-types`

**Response:** `200 OK`, wrapped array of supported payment-type strings.

### 3.13 Get Availability and Pricing

**Endpoint:** `GET /api/v1/reservations/availability`

**Query parameters:** required `propertyId`; either `date` or `arrivalDate` plus
`night`, or `arrivalDate` plus `departureDate`. Optional `numberOfRooms` (1–9),
`groupCode`, `adults`/`adultCount` (minimum 1), `children`/`childCount`
(minimum 0), `company`, `rateCode`, `blockCode`.

**Response:** wrapped `ReservationAvailabilityResponseDto`, including room
availability, rate plans, pricing, and stay dates.

**Errors:** `400` missing date/night or departure before arrival; `422` parameter
validation failure; downstream inventory/rate errors may produce `502`/`500`.

### 3.14 Get Reservation Room Calendar

**Endpoint:** `GET /api/v1/reservations/rooms/calendar`

**Query parameters:** required `propertyId`, `arrivalDate`, `departureDate`;
optional repeated `roomTypes` and optional singular `roomType`.

**Response:** wrapped `ReservationRoomCalendarResponseDto` with room/date booking
and housekeeping statuses.

**Errors:** `400` invalid dates/property; `404` missing property data.

### 3.15 List Reservation Guest Assignments

**Endpoint:** `GET /api/v1/reservation-guests/assignments`

**Query parameters:** required `propertyId`; exactly one lookup selector is
expected among `bookingId`, `confirmationNumber`, or `guestProfileIds`.

**Response:** wrapped array of `ReservationGuestAssignmentDto`.

**Errors:** `400` missing property, nonpositive booking ID, or ambiguous selector;
`404` no assignments.

### 3.16 Make Reservation Guest Primary

**Endpoint:** `PATCH /api/v1/reservation-guests/{reservationGuestId}/primary`

**Path parameters:** positive `reservationGuestId`.

**Request body:** `{"bookingId":456}`.

**Response:** wrapped `ReservationGuestResponseDto`.

**Errors:** `400` invalid request; `404` booking/guest assignment not found;
`409` assignment conflict.

### 3.17 Complete Check-In

**Endpoint:** `POST /api/v1/reservations/bookings/{confirmationNumber}/check-in/complete`

**Path parameters:** `confirmationNumber`.

**Query parameters:** optional `bookingId`.

**Request body:** `{"actor":"frontdesk","businessDate":"2026-10-10",
"targetStatus":"CHECKED_IN"}`. Fields are validated by
`CheckInCompleteRequestDto`.

**Response: `200 OK`**

```json
{"success":true,"message":"Check-in completed successfully","data":{
  "confirmationNumber":"CN001","bookingId":456,"status":"CHECKED_IN"
},"errors":{},"timestamp":"2026-10-10T10:00:00Z"}
```

**Errors:** `400` invalid workflow/state; `404` reservation not found;
`409` check-in conflict.

### 3.18 Complete Check-Out

**Endpoint:** `POST /api/v1/reservations/bookings/{confirmationNumber}/check-out`

**Path parameters:** `confirmationNumber`.

**Query parameters:** optional `bookingId`.

**Request body:** `CheckoutRequestDto` with `actor`, `businessDate`, and optional
`earlyDepartureDate`.

**Response:** `200 OK`, wrapped `CheckoutCompletionResponseDto`.

**Errors:** `400` invalid checkout/payment state; `404` not found; `409` conflict.

### 3.19 Cancel Check-Out

**Endpoint:** `POST /api/v1/reservations/bookings/{confirmationNumber}/check-out/cancel`

**Path parameters:** `confirmationNumber`.

**Query parameters:** optional `bookingId`.

**Request body:** `CheckoutRequestDto`.

**Response:** `200 OK`, wrapped `CheckoutCompletionResponseDto`.

**Errors:** `400` invalid cancellation/state; `404` not found; `409` conflict.

### 3.20 Save Digital Signature

**Endpoint:** `POST /api/v1/saveDigitalSignature`

**Content type:** `multipart/form-data`.

**Form parameters:** required `bookingId`, `confirmationNumber`, `propertyId`,
`file`; optional `checkInChannel` (default `FRONT_DESK`).

**Response:** wrapped `CheckInSignatureResponseDto`.

**Errors:** `400` missing/invalid file or identifiers; `404` booking not found.

### 3.21 Get Digital Signature

**Endpoint:** `GET /api/v1/getDetailsDigitalSignature`

**Query parameters:** required `bookingId`, `confirmationNumber`.

**Response:** `200 OK` binary image with stored content type and inline filename
`signature`.

**Errors:** `404` signature not found; `400` invalid identifiers.

### 3.22 Upload ID Proof

**Endpoint:** `POST /api/v1/uploadIdProofDetails`

**Content type:** `multipart/form-data`.

**Form parameters:** required `bookingId`, `confirmationNumber`, `propertyId`,
`idProofType`, `idProofNumber`, `file`; optional `checkInChannel`.

**Response:** wrapped `IdProofResponseDto`.

**Errors:** `400` invalid form/file; `404` booking not found.

### 3.23 Get ID Proof

**Endpoint:** `GET /api/v1/getUploadIdProofDetails`

**Query parameters:** required `bookingId`, `confirmationNumber`.

**Response:** `200 OK` binary image with inline filename `id-proof`.

**Errors:** `404` ID proof not found; `400` invalid identifiers.

### 3.24 Guest Listing

**Endpoint:** `GET /api/v1/guest-listing/list`

**Query parameters:** arrival/departure listing filters include `propertyId`,
`businessDate`, `search`, `status`, `reservationType`, `city`, `roomStatus`,
`corporateCode`, `roomType`, `floor`, `company`, `sharingStatus`,
`loyaltyMembershipStatus`, pagination `page`/`size`, sorting `sortBy`/`sortDir`,
and `includeOptions`. The controller selects arrival/departure behavior from the
request shape.

**Response:** `ApiResponse<PagedResponse<ReservationArrivalDto|DepartureResponseDto>>`
with page metadata and optional filter options.

**Errors:** `400` invalid filters or dates; `404` property not found.

### 3.25 Frontdesk Dashboard

**Endpoint:** `GET /api/v1/frontdesk/dashboard`

**Query parameters:** required UUID `propertyId` and ISO `businessDate`.

**Response:** wrapped `FrontdeskDashboardResponse` containing KPIs, revenue,
inventory, housekeeping status, room-type overview, guest activity, and source
health statuses.

**Errors:** `400` invalid parameters; `401/403` authentication/role failure;
`500` dashboard aggregation failure.

### 3.26 Mark Room Occupied

**Endpoint:** `POST /api/v1/housekeeping/rooms/check-in`

**Request body:** `HousekeepingRoomStatusRequestDto`:
`propertyId`, `businessDate`, `confirmationNumber`, `bookingId`, `roomNo`.

**Response:** wrapped `HousekeepingRoomStatusResponseDto`.

### 3.27 Mark Room Dirty

**Endpoint:** `POST /api/v1/housekeeping/rooms/check-out`

**Request body:** Same status request fields as Mark Room Occupied.

**Response:** wrapped status response with the room marked dirty.

### 3.28 Manually Update Room Status

**Endpoint:** `PATCH /api/v1/housekeeping/rooms/updateRoom`

**Request body:** `HousekeepingManualStatusUpdateRequestDto`, extending the status
request with required `roomStatus`.

**Response:** wrapped `HousekeepingRoomStatusResponseDto`.

**Errors for 3.26–3.28:** `400` validation/business error; `404` room or booking
not found; `409` invalid status transition.

---

## 4. pms-housekeeping

The normal `/api/**` endpoints require `ADMIN`. Room-master sync and property
deletion are explicitly permitted for internal synchronization.

### 4.1 Housekeeping Dashboard

**Endpoint:** `GET /api/v1/housekeeping/dashboard`

**Query parameters:** required `propertyId`, `businessDate`.

**Response:** `HousekeepingDashboardResponse` with room totals, occupancy,
cleaning and operational counters.

### 4.2 List Housekeeping Rooms

**Endpoint:** `GET /api/v1/housekeeping/rooms`

**Query parameters/model:** `HousekeepingRoomFilterRequest` includes property/date,
search and room/status filters, pagination and sorting fields.

**Response:** `HousekeepingRoomsPageResponse` containing room rows, page metadata,
and filter information.

### 4.3 Housekeeping Calendar

**Endpoint:** `GET /api/v1/housekeeping/rooms/calendar`

**Query parameters:** required `propertyId`, `fromDate`, `toDate`; optional repeated
`roomTypes`.

**Response:** `HousekeepingCalendarResponse` with room types, rooms, dates, and
day-level statuses.

### 4.4 Assignable Rooms

**Endpoint:** `GET /api/v1/housekeeping/assignable-rooms`

**Query parameters:** required `propertyId`, `roomTypeId`, `businessDate`;
optional `limit` (default `50`).

**Response:** array of `AssignableRoomResponse`, such as
`{"roomNumber":"101","roomTypeId":"RT1","status":"CLEAN"}`.

### 4.5 Update Housekeeping Room Details

**Endpoint:** `PATCH /api/v1/housekeeping/rooms/{roomNumber}/updateRoom`

**Path parameters:** `roomNumber`.

**Request body:** `UpdateHousekeepingRoomDetailsRequest`, containing the property,
business date and editable status/attendant/feature fields.

**Response:** `HousekeepingRoomDetailsUpdateResponse`.

### 4.6 Release Reservation Assignment

**Endpoint:** `POST /api/v1/housekeeping/reservations/{confirmationId}/release`

**Path parameters:** `confirmationId`.

**Query parameters:** required `propertyId`; optional `roomNumber`, `arrivalDate`,
`departureDate`.

**Response:** integer count of released assignments.

**Errors for 4.1–4.6:** `400` validation/business error; `404` room/property not
found; `409` optimistic-lock or conflicting status update.

### 4.7 Synchronize Room Master

**Endpoint:** `POST /api/v1/housekeeping/room-master/sync`

**Request body:** `RoomMasterSyncRequest` containing property and room-master data
used to initialize room-day rows.

**Response:**

```json
{"syncedRooms":12,"deactivatedRooms":2}
```

**Authentication:** explicitly permitted by housekeeping security for internal sync.

### 4.8 Delete Property Housekeeping Data

**Endpoint:** `DELETE /api/v1/housekeeping/room-master/properties/{propertyId}`

**Path parameters:** `propertyId`.

**Response:** `{"deleted":true}`.

**Authentication:** explicitly permitted by housekeeping security for internal
property synchronization.

---

## 5. pms-inventory

Most `/api/**` endpoints require `ADMIN`. Inventory reconciliation and property
deletion-check/data paths are explicitly permitted by the module security
configuration.

### 5.1 Get Room-Type Availability

**Endpoint:** `GET /api/v1/inventory/availability`

**Query parameters:** required `propertyId`, `roomTypeId`, `fromDate`, `toDate`.

**Response:** array of:

```json
{"propertyId":"P001","roomTypeId":"RT1","businessDate":"2026-10-10",
 "totalInventory":20,"reservedCount":5,"blockedCount":1,"availableCount":14}
```

**Errors:** `400` missing/invalid values or invalid date range.

### 5.2 Create Inventory Block

**Endpoint:** `POST /api/v1/inventory/blocks`

**Request body:** `CreateInventoryBlockRequest` with property/room-type, date
range, quantity and block metadata.

**Response:** `InventoryBlockResponse` containing block ID, dates, quantity and
status.

### 5.3 Release Inventory Block

**Endpoint:** `POST /api/v1/inventory/blocks/{blockId}/release`

**Path parameters:** `blockId`.

**Response:** updated `InventoryBlockResponse`.

**Errors for 5.2–5.3:** `400` validation/insufficient inventory; `404` block not
found; `409` state conflict.

### 5.4 Get Daily Inventory

**Endpoint:** `GET /api/v1/inventory/daily`

**Query parameters:** required `propertyId`, `roomTypeId`, `businessDate`.

**Response:** `DailyInventoryResponse` with total, reserved, blocked and available
counts.

### 5.5 Check Property Deletion

**Endpoint:** `GET /api/v1/inventory/properties/{propertyId}/deletion-check`

**Path parameters:** `propertyId`.

**Query parameters:** required `businessDate`.

**Response:**

```json
{"success":true,"message":"Property deletion check completed","data":{
  "hasActiveReservations":false
}}
```

### 5.6 Delete Property Inventory

**Endpoint:** `DELETE /api/v1/inventory/properties/{propertyId}`

**Path parameters:** `propertyId`.

**Response:** `200 OK`, common inventory wrapper with `data: null`.

**Authentication:** explicitly permitted for property synchronization.

### 5.7 Reconcile Inventory

**Endpoint:** `POST /api/v1/inventory/reconciliation`

**Request body:** `InventoryReconciliationRequest` containing property, room type,
business date and inventory counts from external property-master data.

**Response:** `{"affectedRows":3}`.

**Authentication:** explicitly permitted by inventory security.

### 5.8 Reserve Inventory

**Endpoint:** `POST /api/v1/inventory/reservations`

**Request body:** `ReserveInventoryRequest` containing confirmation number,
property/room type, arrival/departure dates and quantity.

**Response:** `InventoryReservationResponse` with confirmation, reserved quantity,
dates and status.

### 5.9 Release Reserved Inventory

**Endpoint:** `POST /api/v1/inventory/reservations/{confirmationNumber}/release`

**Path parameters:** `confirmationNumber`.

**Response:** updated `InventoryReservationResponse`.

### 5.10 Change Assigned Room Type

**Endpoint:** `PUT /api/v1/inventory/reservations/{confirmationNumber}/assigned-room-type`

**Path parameters:** `confirmationNumber`.

**Request body:** `ChangeAssignedRoomTypeRequest` containing the new room type and
reservation context.

**Response:** updated `InventoryReservationResponse`.

**Errors for 5.8–5.10:** `400` invalid request/insufficient inventory; `404` no
reservation; `409` inventory/state conflict.

---

## 6. property-service

The property service has published-property, wizard/draft, content, finance,
payment, tax, room, upload, and inventory-sync APIs. `/api/**` is restricted to
`ADMIN`; documentation/upload/error paths are public. Inventory-sync endpoints
are under an internal path but use the service's configured security rules.

### 6.1 Create Draft Property

**Endpoint:** `POST /api/property/drafts/createDraft`

**Request body:** `CreateDraftRequest` with the initial property/wizard payload.

**Response: `200 OK`**

```json
{"success":true,"message":"Draft property created","data":{
  "draftId":1,"propertyId":"P001","status":"DRAFT"
}}
```

### 6.2 Save Draft

**Endpoint:** `PUT /api/property/drafts/saveDraft/{draftId}`

**Path parameters:** `draftId`.

**Request body:** `SaveDraftRequest`, containing the draft wizard payload.

**Response:** wrapped `DraftResponse`.

### 6.3 List Draft and Published Properties

**Endpoint:** `GET /api/property/drafts/getAllProperties`

**Query parameters:** optional repeated `status` values matching `DraftStatus`.

**Response:** wrapped array of `DraftResponse`.

**Errors:** `400` invalid status filter.

### 6.4 Get Draft

**Endpoint:** `GET /api/property/drafts/getDraft/{draftId}`

**Path parameters:** `draftId`.

**Response:** wrapped `DraftResponse`.

### 6.5 List Wizard Property Options

**Endpoint:** `GET /api/property/drafts/wizard/properties`

**Response:** wrapped array of `WizardPropertyOptionResponse`.

### 6.6 Get Wizard Draft for Published Property

**Endpoint:** `GET /api/property/drafts/wizard/properties/{propertyId}`

**Path parameters:** `propertyId`.

**Response:** wrapped `DraftResponse` derived from the selected published property.

### 6.7 Publish Draft

**Endpoint:** `POST /api/property/drafts/{draftId}/publish`

**Path parameters:** `draftId`.

**Response:**

```json
{"success":true,"message":"Draft property published","data":{
  "draftId":1,"propertyId":"P001","status":"PUBLISHED"
}}
```

**Errors:** `400` incomplete draft; `404` draft not found; `409` publish/state
conflict; downstream sync failures may be surfaced as `5xx`.

### 6.8 Delete Draft

**Endpoint:** `DELETE /api/property/drafts/deleteDraft/{draftId}`

**Path parameters:** `draftId`.

**Response:** wrapped `null` data.

**Errors:** `400` draft is not editable; `404` not found; `403` ownership failure.

### 6.9 Get Published Property

**Endpoint:** `GET /api/property/getPublishedProperty/{propertyId}`

**Path parameters:** `propertyId`.

**Response:** wrapped `PropertyResponse` with property identity, metadata and
published configuration.

### 6.10 List Published Properties

**Endpoint:** `GET /api/property/getAllPublishedProperties`

**Response:** wrapped array of `PropertyResponse` owned by the current user.

### 6.11 Delete Published Property

**Endpoint:** `DELETE /api/property/deletePublishedProperty/{propertyId}`

**Path parameters:** `propertyId`.

**Response:** wrapped `null` data.

**Errors:** `400/403` ownership or deletion rule failure; `404` not found;
`409` active reservations prevent deletion.

### 6.12 Upload Image

**Endpoint:** `POST /api/uploads/images`

**Content type:** `multipart/form-data`.

**Form parameter:** required `file`.

**Response:**

```json
{"success":true,"message":"Image uploaded","data":{
  "fileName":"room.jpg","url":"/uploads/abc-room.jpg"
}}
```

**Errors:** `400` empty/unsupported file; `500` storage failure.

### 6.13 Property Content Summary

**Endpoint:** `GET /api/content/properties/{propertyId}/summary`

**Response:** wrapped `ContentSummaryResponse`.

### 6.14 List Content Overviews

**Endpoint:** `GET /api/content/properties/{propertyId}/overviews`

**Response:** wrapped array of `ContentOverviewResponse`.

### 6.15 Get Content Overview

**Endpoint:** `GET /api/content/properties/{propertyId}/overviews/{overviewId}`

**Path parameters:** `propertyId`, `overviewId`.

**Response:** wrapped `ContentOverviewResponse`.

### 6.16 Create Content Overview

**Endpoint:** `POST /api/content/properties/{propertyId}/overviews`

**Request body:** `ContentOverviewRequest` (overview title/content and associated
property content fields).

**Response:** wrapped created `ContentOverviewResponse`.

### 6.17 Update Content Overview

**Endpoint:** `PUT /api/content/properties/{propertyId}/overviews/{overviewId}`

**Request body:** `ContentOverviewRequest`.

**Response:** wrapped updated `ContentOverviewResponse`.

### 6.18 Delete Content Overview

**Endpoint:** `DELETE /api/content/properties/{propertyId}/overviews/{overviewId}`

**Response:** wrapped `null` data.

### 6.19 Finance Summary

**Endpoint:** `GET /api/finance/properties/{propertyId}/summary`

**Response:** wrapped `FinanceSummaryResponse`.

### 6.20 List Chart of Accounts

**Endpoint:** `GET /api/finance/properties/{propertyId}/accounts`

**Response:** wrapped array of `ChartOfAccountResponse`.

### 6.21 Get Chart of Account

**Endpoint:** `GET /api/finance/properties/{propertyId}/accounts/{accountId}`

**Path parameters:** `propertyId`, `accountId`.

**Response:** wrapped `ChartOfAccountResponse`.

### 6.22 Create Chart of Account

**Endpoint:** `POST /api/finance/properties/{propertyId}/accounts`

**Request body:** `ChartOfAccountRequest` with account code/name/type and account
configuration fields.

**Response:** wrapped `ChartOfAccountResponse`.

### 6.23 Update Chart of Account

**Endpoint:** `PUT /api/finance/properties/{propertyId}/accounts/{accountId}`

**Request body:** `ChartOfAccountRequest`.

**Response:** wrapped updated `ChartOfAccountResponse`.

### 6.24 Delete Chart of Account

**Endpoint:** `DELETE /api/finance/properties/{propertyId}/accounts/{accountId}`

**Response:** wrapped `null` data.

### 6.25 Payment Summary

**Endpoint:** `GET /api/payments/properties/{propertyId}/summary`

**Response:** wrapped `PaymentSummaryResponse`.

### 6.26 List Payment Methods

**Endpoint:** `GET /api/payments/properties/{propertyId}/methods`

**Response:** wrapped array of `PaymentMethodResponse`.

### 6.27 Get Payment Method

**Endpoint:** `GET /api/payments/properties/{propertyId}/methods/{methodId}`

**Path parameters:** `propertyId`, `methodId`.

**Response:** wrapped `PaymentMethodResponse`.

### 6.28 Create Payment Method

**Endpoint:** `POST /api/payments/properties/{propertyId}/methods`

**Request body:** `PaymentMethodRequest` with method name/type, active state and
configuration fields.

**Response:** wrapped `PaymentMethodResponse`.

### 6.29 Update Payment Method

**Endpoint:** `PUT /api/payments/properties/{propertyId}/methods/{methodId}`

**Request body:** `PaymentMethodRequest`.

**Response:** wrapped updated `PaymentMethodResponse`.

### 6.30 Delete Payment Method

**Endpoint:** `DELETE /api/payments/properties/{propertyId}/methods/{methodId}`

**Response:** wrapped `null` data.

### 6.31 Tax Summary

**Endpoint:** `GET /api/taxes/properties/{propertyId}/summary`

**Response:** wrapped `TaxSummaryResponse`.

### 6.32 List Tax Rules

**Endpoint:** `GET /api/taxes/properties/{propertyId}/rules`

**Response:** wrapped array of `TaxRuleResponse`.

### 6.33 Get Tax Rule

**Endpoint:** `GET /api/taxes/properties/{propertyId}/rules/{ruleId}`

**Path parameters:** `propertyId`, `ruleId`.

**Response:** wrapped `TaxRuleResponse`.

### 6.34 Create Tax Rule

**Endpoint:** `POST /api/taxes/properties/{propertyId}/rules`

**Request body:** `TaxRuleRequest` with rule name/type, percentage or fixed amount,
amount bounds, room/category scope and active state.

**Response:** wrapped `TaxRuleResponse`.

### 6.35 Update Tax Rule

**Endpoint:** `PUT /api/taxes/properties/{propertyId}/rules/{ruleId}`

**Request body:** `TaxRuleRequest`.

**Response:** wrapped updated `TaxRuleResponse`.

### 6.36 Delete Tax Rule

**Endpoint:** `DELETE /api/taxes/properties/{propertyId}/rules/{ruleId}`

**Response:** wrapped `null` data.

### 6.37 Room Summary

**Endpoint:** `GET /api/rooms/properties/{propertyId}/summary`

**Response:** wrapped `RoomSummaryResponse`.

### 6.38 List Inventory Rooms

**Endpoint:** `GET /api/rooms/properties/{propertyId}/inventory-rooms`

**Response:** wrapped array of `InventoryRoomResponse`.

### 6.39 List Room Outlet Types

**Endpoint:** `GET /api/rooms/properties/{propertyId}/room-outlet-types`

**Response:** wrapped array of `RoomOutletTypeResponse`.

### 6.40 Get Inventory Room

**Endpoint:** `GET /api/rooms/properties/{propertyId}/inventory-rooms/{roomId}`

**Path parameters:** `propertyId`, `roomId`.

**Response:** wrapped `InventoryRoomResponse`.

### 6.41 Create Inventory Room

**Endpoint:** `POST /api/rooms/properties/{propertyId}/inventory-rooms`

**Request body:** `InventoryRoomRequest` with room number/name/type, floor,
outlet/room-type and inventory configuration fields.

**Response:** wrapped `InventoryRoomResponse`.

### 6.42 Update Inventory Room

**Endpoint:** `PUT /api/rooms/properties/{propertyId}/inventory-rooms/{roomId}`

**Request body:** `InventoryRoomRequest`.

**Response:** wrapped updated `InventoryRoomResponse`.

### 6.43 Delete Inventory Room

**Endpoint:** `DELETE /api/rooms/properties/{propertyId}/inventory-rooms/{roomId}`

**Response:** wrapped `null` data.

### 6.44 Retry Inventory Synchronization

**Endpoint:** `POST /api/v1/internal/inventory-sync/{propertyId}/retry`

**Path parameters:** `propertyId`.

**Request:** No body. The incoming `Authorization` header is forwarded to the
inventory service.

**Response:** `ApiResponse<InventorySyncStatusResponse>`.

### 6.45 Inventory Synchronization Status

**Endpoint:** `GET /api/v1/internal/inventory-sync/{propertyId}/status`

**Path parameters:** `propertyId`.

**Response:** `ApiResponse<InventorySyncStatusResponse>`.

**Errors for 6.13–6.45:** generally `404` missing property/resource, `400` invalid
request, `409` duplicate or dependent-resource conflict, and `500` persistence or
downstream synchronization failure.

---

## 7. rate-management

All non-documentation endpoints require `ADMIN`. Responses are mostly raw DTOs or
arrays rather than a common wrapper.

### 7.1 Create Rate Plan

**Endpoint:** `POST /api/rate-plans/create-rate-plan/property/{propertyId}`

**Request body:** `RatePlanRequestDTO`:
`name`, `code`, `occupancyType`, `mealOption`, `inclusion`, `type`, date range,
`activeDaysOfWeek`, `applicableRoomTypeIds`, `calculationMethod`,
`adjustmentValue`, `manualAmount`, `manualPricingByOccupancy`,
`parentRatePlanId`, and `policyId`.

**Response:** `RatePlanResponseDTO`.

### 7.2 Update Rate Plan

**Endpoint:** `PUT /api/rate-plans/update-rate-plan/property/{propertyId}/{id}`

**Path parameters:** `propertyId`, `id`.

**Request body:** `RatePlanRequestDTO`.

**Response:** updated `RatePlanResponseDTO`.

### 7.3 List Rate Plans

**Endpoint:** `GET /api/rate-plans/get-all-rate-plans/property/{propertyId}`

**Response:** array of `RatePlanResponseDTO`.

### 7.4 Delete Rate Plan

**Endpoint:** `DELETE /api/rate-plans/delete-rate-plan/property/{propertyId}/{id}`

**Path parameters:** `propertyId`, `id`.

**Response:** `204 No Content`.

### 7.5 Change Rate Plan Status

**Endpoint:** `PATCH /api/rate-plans/change-rate-plan-status/property/{propertyId}/{id}/status`

**Query parameters:** required `status` (`RatePlanStatus`).

**Response:** updated `RatePlanResponseDTO`.

### 7.6 Get Available Rate Plans

**Endpoint:** `GET /api/rate-plans/available/property/{propertyId}`

**Query parameters:** required `roomTypeId`, `occupancyType`, `stayDate`;
optional `mealOption`.

**Response:** array of applicable `RatePlanResponseDTO`.

### 7.7 Calculate Rate Plan Price

**Endpoint:** `GET /api/rate-plans/final-price/property/{propertyId}/{id}/calculated-price`

**Path parameters:** `propertyId`, rate-plan `id`.

**Query parameters:** required `roomTypeId`; optional `guestCount` and
`occupancyType`. If occupancy is omitted, `guestCount` is converted to
`"<count> Guest"`.

**Response:**

```json
{"ratePlanId":10,"masterBarAmount":2500.0,"finalAmount":2800.0}
```

### 7.8 Map Policies to Rate Plan

**Endpoint:** `PATCH /api/rate-plans/map/policy-to-rate-plan/property/{propertyId}/{ratePlanId}`

**Query parameters:** required repeated/list `policyId`.

**Response:** updated `RatePlanResponseDTO`.

### 7.9 Unmap Policies from Rate Plan

**Endpoint:** `PATCH /api/rate-plans/unmap/policy-from-rate-plan/property/{propertyId}/{ratePlanId}`

**Query parameters:** required repeated/list `policyId`.

**Response:** updated `RatePlanResponseDTO`.

**Errors for 7.1–7.9:** `400` invalid rate-plan fields, occupancy, dates or mapping;
`404` property/rate plan/room type not found; `409` duplicate or state conflict.

### 7.10 Create Master Room

**Endpoint:** `POST /api/master-rooms/create-master-room/property/{propertyId}`

**Request body:** `MasterRoomRequestDTO` with `name`, `mealOption`, `inclusion`,
date range, `pricingList`, and `roomTypeMappings`.

**Response:** `MasterRoomResponseDTO`.

### 7.11 Update Master Room

**Endpoint:** `PUT /api/master-rooms/update-master-room/property/{propertyId}/{id}`

**Path parameters:** `propertyId`, master-room `id`.

**Request body:** `MasterRoomRequestDTO`.

**Response:** updated `MasterRoomResponseDTO`.

### 7.12 List Master Rooms

**Endpoint:** `GET /api/master-rooms/get-all-master-room/property/{propertyId}`

**Response:** array of `MasterRoomResponseDTO`.

### 7.13 Delete Master Room

**Endpoint:** `DELETE /api/master-rooms/delete-master-room/property/{propertyId}/{id}`

**Response:** `204 No Content`.

### 7.14 Add or Update Occupancy Pricing

**Endpoint:** `POST /api/master-rooms/update-pricing-by-occupancy/property/{propertyId}/{id}/pricing`

**Request body:** `{"occupancyType":"2 Guest","price":3000.0}`.

**Response:** `MasterRoomPricingResponseDTO`.

### 7.15 List Master-Room Pricing

**Endpoint:** `GET /api/master-rooms/{id}/pricing`

**Path parameters:** master-room `id`.

**Response:** array of `MasterRoomPricingResponseDTO`.

### 7.16 Map Room Type to Master Room

**Endpoint:** `POST /api/master-rooms/map-room-type/property/{propertyId}/{id}`

**Request body:** `MasterRoomRoomTypeMappingRequestDTO` with `roomTypeId`,
`differentialType`, and `differentialValue`.

**Response:** `MasterRoomRoomTypeMappingResponseDTO`.

### 7.17 List Master-Room Mappings

**Endpoint:** `GET /api/master-rooms/get-room-type-mapping/{id}/mappings`

**Response:** array of `MasterRoomRoomTypeMappingResponseDTO`.

### 7.18 List Property Room-Type Mappings

**Endpoint:** `GET /api/master-rooms/property/{propertyId}/mappings`

**Response:** array of `PropertyRoomTypeMappingResponseDTO`, including inherited
rates and mapping state.

### 7.19 Validate Room-Type Mappings

**Endpoint:** `POST /api/master-rooms/validate-mappings`

**Request body:** JSON array of active room-type IDs, for example `[101,102]`.

**Response:** raw boolean.

### 7.20 Override Room-Type Pricing

**Endpoint:** `POST /api/master-rooms/room-type/{roomTypeId}/override-pricing`

**Query parameters:** required `occupancyType`, `newPrice`.

**Response:** `200 OK` with an empty body.

### 7.21 Break Pricing Inheritance

**Endpoint:** `POST /api/master-rooms/room-type/{roomTypeId}/break-inheritance`

**Path parameters:** `roomTypeId`.

**Response:** `200 OK` with an empty body.

---

## 8. pms-folio-billing

All billing endpoints require `ADMIN` except
`GET /api/v1/billingFolio/getFolioDetails`, which is explicitly permitted.

### 8.1 List Folio Billing Rows

**Endpoint:** `GET /api/v1/billingFolio/getFolioBilling`

**Query parameters:** optional `roomNumber`, `guestName`, `company`/`companyName`,
`confirmationNumber`, `bookingId`, `checkInDate`, `checkOutDate`, `propertyId`.

**Response:** array of `FolioBillingRow`.

### 8.2 Create Folio

**Endpoint:** `POST /api/v1/billingFolio/addFolio`

**Request body:** `FolioCreateRequest` with `confirmationNumber` and optional
`bookingId`, `roomNo`, `guestName`, and `userId`.

**Response:** `FolioCreateResponse` with folio code, guest/room, totals and
timestamps.

### 8.3 Get Billing Details

**Endpoint:** `GET /api/v1/billingFolio/getBillingDetails`

**Query parameters:** optional `confirmationNumber` or alias `confirmationNo`,
`bookingId`, `roomNo`, `guestName`.

**Response:** `BillingDetailsResponse` with totals, folio codes, guest, stay,
room, reservation, comments and status information.

### 8.4 Get Folio Dashboard

**Endpoint:** `GET /api/v1/billingFolio/folioDashboard`

**Query parameters:** optional `confirmationNumber`.

**Response:** `FolioDashboardResponse` containing folio totals and dashboard
metrics.

### 8.5 Get Folio Details

**Endpoint:** `GET /api/v1/billingFolio/getFolioDetails`

**Query parameters:** optional `confirmationNumber` or `confirmationNo`, `bookingId`,
and `propertyId` (the controller passes confirmation/booking to the service).

**Response:** `FolioDetailsResponse`, including guest, folios, transactions and
summary totals.

**Authentication:** explicitly permitted by billing security.

### 8.6 Add Charge

**Endpoint:** `POST /api/v1/billingFolio/addCharge`

**Request body:** `FolioChargePostRequest`:
`confirmationNumber`, `bookingId`, `folioId`, `folioName`, `guestName`, `roomNo`,
`transactionType`, `category`, `description`, amount/charges/credit,
`postingDate`, `userId`, and optional nested `transaction`.

**Response:** `FolioChargePostResponse` with transaction and updated folio
summary/checkout state.

### 8.7 Get Charge Options

**Endpoint:** `GET /api/v1/billingFolio/chargeOptions`

**Request:** None.

**Response:**

```json
{"transactionTypes":["Charges","Payment","Adjustment","Refund"],
 "categoriesByTransactionType":{"Charges":["Accomodation","Housekeeping",
 "Wellness","F&B","Transport","Miscellaneous"]}}
```

### 8.8 Adjust Charge

**Endpoint:** `POST /api/v1/billingFolio/adjustCharge`

**Request body:** `FolioChargeAdjustmentRequest` with folio/transaction identity,
adjustment type, amount/reason and user context.

**Response:** `FolioChargeAdjustmentResponse`.

### 8.9 Update Transaction Amount

**Endpoint:** `PATCH /api/v1/billingFolio/transaction/amount`

**Request body:** `FolioTransactionAmountUpdateRequest` with transaction identity,
new amount and user context.

**Response:** `FolioTransactionAmountUpdateResponse`.

### 8.10 Allocate Payment

**Endpoint:** `POST /api/v1/billingFolio/allocatePayment`

**Request body:** `FolioPaymentAllocationRequest` with payment reference/amount and
allocation target lines.

**Response:** `FolioPaymentAllocationResponse` containing allocation results and
remaining amount.

### 8.11 Payment Allocation History

**Endpoint:** `GET /api/v1/billingFolio/paymentAllocationHistory`

**Query parameters:** optional `confirmationNumber`, `paymentReference`.

**Response:** array of `PaymentAllocationHistoryEntry`.

### 8.12 Generate Folio Document

**Endpoint:** `POST /api/v1/billingFolio/generateDocument`

**Request body:** `FolioDocumentGenerateRequest` with confirmation/booking
selection, document type and requested document metadata.

**Response:** `FolioDocumentGenerateResponse` containing document ID, filename and
document metadata.

### 8.13 Download Folio Document

**Endpoint:** `GET /api/v1/billingFolio/documents/{documentId}/download`

**Path parameters:** `documentId`.

**Response:** `200 OK`, UTF-8 document bytes with attachment disposition.

### 8.14 Print Folio Document

**Endpoint:** `GET /api/v1/billingFolio/documents/{documentId}/print`

**Path parameters:** `documentId`.

**Response:** `200 OK`, document HTML/text with inline disposition.

### 8.15 Folio Document Audit History

**Endpoint:** `GET /api/v1/billingFolio/documentAuditHistory`

**Query parameters:** optional `confirmationNumber`, `documentType`.

**Response:** array of `FolioDocumentAuditEntry`.

**Errors for 8.1–8.15:** `400` invalid financial request; `404` folio, transaction
or document not found; `409` balance/allocation/state conflict; `500` reservation
service or persistence failure.

---

## 9. Policy

The Policy service protects `/api/**` with `ADMIN` and returns
`{"status":"success","message":"...","data":...}`.

### 9.1 Create Policy

**Endpoint:** `POST /api/v1/policies/createPolicy`

**Request body:** `PolicyDto`:
`policyName`, `policyType`, `serviceType`, `usedBy`, `policyCode`,
`policyCategory`, `priority`, `description`, `effectiveDate`, `effectiveTo`,
`status`, `action`, `propertyId`, and `createdByUser`.

**Response: `201 Created`**

```json
{"status":"success","message":"Policy created successfully","data":{
  "id":1,"policyName":"Breakfast","policyCode":"BRK","status":"DRAFT",
  "propertyId":"P001","policyCount":1
}}
```

Published policies require the required policy fields, property ID and creator
according to the service validator.

### 9.2 List Policies

**Endpoint:** `GET /api/v1/policies/getAllPolicies`

**Query parameters:** optional `status` (`Status`).

**Response:** wrapped `PolicyListResponse` containing `policies`,
`totalPolicies`, `activePolicies`, `draftPolicies`, and `inactivePolicies`.

### 9.3 Get Policy

**Endpoint:** `GET /api/v1/policies/getPoliciesById/{id}`

**Path parameters:** `id`.

**Response:** wrapped `PolicyDto`.

### 9.4 List Policies by Property

**Endpoint:** `GET /api/v1/policies/getPoliciesByPropertyId/{propertyId}`

**Path parameters:** `propertyId`.

**Response:** wrapped `PolicyListResponse`.

**Errors:** `400` blank property ID; `404` missing policy/property.

### 9.5 Update Policy

**Endpoint:** `PUT /api/v1/policies/updatePolicy/{id}`

**Path parameters:** `id`.

**Request body:** `PolicyDto`; status transitions trigger status-specific
validation.

**Response:** wrapped updated `PolicyDto`.

### 9.6 Map Policy to Property

**Endpoint:** `PUT /api/v1/policies/mapPolicyToProperty/{policyId}/{propertyId}`

**Path parameters:** `policyId`, `propertyId`.

**Request:** No body.

**Response:** wrapped mapped `PolicyDto`.

### 9.7 Unmap Policy from Property

**Endpoint:** `PUT /api/v1/policies/unmapPolicyFromProperty/{policyId}`

**Path parameters:** `policyId`.

**Request:** No body.

**Response:** wrapped unmapped `PolicyDto`.

### 9.8 Delete Policy

**Endpoint:** `DELETE /api/v1/policies/deletePolicy/{id}`

**Path parameters:** `id`.

**Response:** `200 OK`, wrapped `null` data.

**Errors for 9.1–9.8:** `400` validation failure; `404` policy/property not found;
`409` duplicate policy code or mapping conflict.

---

## API Relationships

### Reservation booking flow

```text
GET  /api/v1/reservations/availability
        ↓
POST /api/v1/inventory/reservations
        ↓
POST /api/v1/reservations/bookings
        ↓
POST /api/v1/reservations/bookings/{confirmationNumber}/check-in/complete
        ↓
POST /api/v1/reservations/bookings/{confirmationNumber}/check-out
        ↓
POST /api/v1/inventory/reservations/{confirmationNumber}/release
```

Guest profile lookup/creation and reservation-guest assignment are used alongside
booking creation. Folio billing reads reservation summaries and maintains folio
charges/payments.

### Property setup flow

```text
POST /api/property/drafts/createDraft
        ↓
PUT  /api/property/drafts/saveDraft/{draftId}
        ↓
POST /api/property/drafts/{draftId}/publish
        ↓
inventory/housekeeping room-master synchronization
```

Content, finance, payment, tax and room APIs manage the published property
configuration used by reservation and rate-management integrations.

---

## API Summary

| Repository | API count | Main API areas |
|---|---:|---|
| pms-auth | 4 | Registration, login, refresh, logout |
| pms-guest | 7 | Guest lookup, CRUD, search, reservation-linked details |
| pms-reservation | 28 | Booking, availability, check-in/out, guests, documents, listing, dashboard |
| pms-housekeeping | 8 | Dashboard, rooms, calendar, assignments, room master sync |
| pms-inventory | 10 | Availability, blocks, daily inventory, reconciliation, reservations |
| property-service | 45 | Draft/publish, property, uploads, content, finance, payments, taxes, rooms, sync |
| rate-management | 21 | Rate plans, master rooms, pricing, mappings |
| pms-folio-billing | 15 | Folios, charges, allocations, documents, audit history |
| Policy | 8 | Policy CRUD and property mapping |
| **Total** | **146** | |

The counts above exclude class-level `@RequestMapping`, commented-out mappings, and
non-controller methods.

### Suspicious or incomplete endpoints

- `GET /api/rate-plans/{id}` is commented out in `RatePlanController`; rate plans
  have list/create/update/delete but no active single-resource GET.
- `PATCH /api/rate-plans/map/policy-to-rate-plan/...` mutates a rate plan but the
  service method does not explicitly save the entity, unlike the unmap method.
- `GET /api/v1/billingFolio/getFolioDetails` accepts `propertyId`, but the current
  controller does not pass it to the service.
- Folio transaction/allocation/document state is partly held in process-local
  maps; document download/print therefore depends on the running instance.
- Several create/update APIs in `property-service` and `rate-management` return
  `200 OK` for creation rather than `201 Created`; this reflects the current
  controller implementation.
- The rate-plan and master-room controllers do not use `@Valid` on request bodies,
  so validation depends on service logic.
- The housekeeping release endpoint accepts optional dates without explicit
  `@DateTimeFormat`, making date parsing behavior less explicit than other
  date-based endpoints.

### APIs with unclear or limited request/response documentation in code

- `property-service` CRUD controllers mostly rely on DTO types and have no
  controller-level OpenAPI request/response annotations.
- `pms-folio-billing` exposes several large financial DTOs without controller
  validation descriptions; requiredness is enforced in DTO/service code.
- `rate-management` request DTOs are documented here from fields, but most
  controller methods do not declare validation annotations or error schemas.
- `pms-reservation` guest-listing behavior is selected from a shared request
  object; callers should verify whether arrival or departure filters are intended
  for a specific request.

### HTTP-method observations

- Policy mapping/unmapping uses `PUT` with no request body, which is consistent
  with idempotent association replacement only if repeated calls remain safe.
- Rate-plan policy mapping uses `PATCH` with query-list identifiers and is a
  partial association mutation.
- Master-room pricing overrides and inheritance breaking use `POST` for commands,
  matching the current command-style implementation.
- Reservation check-in/check-out use `POST` for workflow commands rather than
  treating them as simple resource updates.
