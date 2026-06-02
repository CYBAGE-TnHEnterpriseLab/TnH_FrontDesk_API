# Arrival Screen API Samples

Base URL: `http://localhost:8090`

## Operational Notes

- Local cache is refreshed from upstream on every search call for the requested propertyId + businessDate.
- Duplicate records are prevented by DB-level unique key on propertyId + business date + confirmation number.
- Upstream records missing mandatory fields (confirmation number, guest name, check-in/check-out) are skipped during sync.
- Refresh policy is configurable via `arrivals.search.sync-mode` (`always` default, optional `cache-miss`).

## 1) List Arrivals (Backend Filtering + Sorting + Pagination)

Endpoint: `GET /api/v1/arrivals/list`

Sample request:

```text
http://localhost:8090/api/v1/arrivals/list?propertyId=PROP001&businessDate=2026-06-01&search=smith&status=DNM&reservationType=Guaranteed&city=Mumbai&roomStatus=Clean&corporateCode=CORP001&roomType=Deluxe%20King&company=ABC%20Travels&sharingStatus=Y&loyaltyMembershipStatus=Gold%20Member&page=0&size=20&sortBy=checkInDate&sortDir=asc
```

Response:

```json
{
  "success": true,
  "message": "Arrival list fetched successfully",
  "data": {
    "propertyId": "PROP001",
    "businessDate": "2026-05-28",
    "content": [
      {
        "id": 1,
        "propertyId": "PROP001",
        "status": "DNM",
        "salutation": "Mr.",
        "firstName": "John",
        "lastName": "Smith",
        "roomNo": "305",
        "reservationType": "Guaranteed",
        "city": "Mumbai",
        "rateCode": "BAR",
        "checkInDate": "2026-05-28",
        "checkOutDate": "2026-05-31",
        "roomNights": 3,
        "roomStatus": "Clean",
        "corporateCode": "CORP001",
        "roomType": "Deluxe King",
        "confirmationNumber": "CNF458721",
        "company": "ABC Travels",
        "sharingStatus": "Y",
        "floor": 3,
        "loyaltyMembershipStatus": "Gold Member"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "sortBy": "checkInDate",
    "sortDir": "asc"
  },
  "errors": null,
  "timestamp": "2026-05-28T09:32:18.062+05:30"
}
```

## Contract Decision

- Use `GET /api/v1/arrivals/list` as the single arrival retrieval API.
- Backend is the source of truth for filtering, sorting, and pagination.
- UI should send criteria via query parameters and render response as-is.
- Never fetch all records in one call; always use pagination (`page`, `size`).
- Recommended default page size is `20`, with server-side max `100`.
- Supported `sortBy`: `guestName`, `roomNo`, `checkInDate`, `roomType`, `company`, plus other technical fields (`firstName`, `lastName`, `checkOutDate`, etc.).

## Swagger UI

- URL: `http://localhost:8090/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8090/api-docs`

## Local Dummy Testing (No Booking Engine Required)

Run app with the `dummy` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dummy
```

What this mode does:

- Uses in-memory H2 database (no PostgreSQL needed)
- Redirects Reservation Service call to an internal mock endpoint in the same app
- Generates deterministic arrival rows for the provided `propertyId` and `businessDate`

Try directly with GET (auto-sync will happen if cache is empty):

```text
http://localhost:8090/api/v1/arrivals/list?propertyId=PROP001&businessDate=2026-05-28&search=smith&page=0&size=20&sortBy=checkInDate&sortDir=asc
```
