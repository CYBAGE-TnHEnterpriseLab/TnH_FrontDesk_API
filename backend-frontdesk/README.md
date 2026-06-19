# Property Service (Draft JSON -> Normalized Publish)

Backend MVP for a 7-step Property Wizard.

## What this implements
- Draft data stored as JSON in `property_draft`.
- Publish converts draft JSON into normalized tables across:
  - property profile and operating timings
  - content (property overview, guest services & amenities, nearby location & accessibility)
  - room setup (room types, floors, generated inventory)
  - finance mappings (chart of accounts, revenue mappings)
  - payment methods and gateway config
  - tax rules
- APIs:
  - `POST /api/property/drafts`
  - `PUT /api/property/drafts/{draftId}`
  - `GET /api/property/drafts/{draftId}`
  - `POST /api/property/drafts/{draftId}/publish`
  - `POST /api/uploads/images` (local image upload for demo)

## Quick start
```powershell
mvn clean test
mvn spring-boot:run
```

## Login (demo hardcoded user)
- Login endpoint: `POST /api/auth/login`
- Credentials: `admin` / `admin123`
- All `/api/**` endpoints except `/api/auth/login` require `Authorization: Bearer <token>`.

Example login:
```bash
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## Local image upload (demo mode)
- Upload endpoint: `POST /api/uploads/images`
- Form field name: `file`
- Allowed formats: `jpg`, `jpeg`, `png`, `webp`, `gif`
- Max upload size: 5 MB (`app.upload.max-bytes`)
- Uploaded files are stored under local `uploads/` and served from `/uploads/**`

Example upload with curl:
```bash
curl -X POST http://localhost:8083/api/uploads/images \
  -F "file=@/path/to/hero-image.png"
```

Use the returned `data.url` in wizard payload image fields, for example:
- `content.propertyOverview.propertyHeroImage`
- `roomsAndOutlets.propertyAreas[].images[]`
- `roomsAndOutlets.roomTypes[].images[]`

## Sample payload for create/update draft
Property Details fields kept for publish normalization:
- `propertyName`, `propertyType`, `propertyCode`
- `totalNoOfRooms`, `totalNoOfFloors`, `website`
- `addressLine1`, `addressLine2`, `city`, `state`, `country`, `zipCode`
- `timeZone`, `primaryContactName`, `phoneNumber`
- `checkInTime`, `checkOutTime`, `nightAuditTime`

```json
{
  "schemaVersion": 1,
  "wizardData": {
    "propertyDetails": {
      "propertyName": "Sea View Villa",
      "propertyCode": "SVV-GOA-001",
      "propertyType": "HOTEL",
      "totalNoOfRooms": 20,
      "totalNoOfFloors": 3,
      "addressLine1": "Beach Road, Dona Paula",
      "addressLine2": "Near Jetty",
      "city": "Panaji",
      "state": "Goa",
      "country": "India",
      "zipCode": "403001",
      "website": "seaviewvilla.com",
      "primaryContactName": "Nikhil",
      "phoneNumber": "+919999999999",
      "timeZone": "Asia/Kolkata",
      "nightAuditTime": "02:00",
      "checkInTime": "13:00",
      "checkOutTime": "11:00"
    },
    "content": {
      "propertyOverview": {
        "propertyHeroImage": "/uploads/example-hero.png",
        "propertyDescription": "Modern stay near key landmarks"
      },
      "guestServicesAndAmenities": {
        "standardAmenities": [
          { "code": "SWIMMING_POOL", "enabled": true }
        ],
        "paidServices": [
          { "code": "AIRPORT_PICKUP", "enabled": true }
        ],
        "onDemandServices": [
          { "code": "EARLY_CHECKIN", "enabled": true }
        ]
      },
      "nearbyLocationAccessibility": {
        "transportConnectivity": [
          { "type": "AIRPORT", "name": "Pune Airport", "distance": 9.5, "distanceUnit": "KM", "travelTime": 30, "travelTimeUnit": "MIN" }
        ],
        "otherLandmarks": [
          { "type": "SHOPPING", "name": "City Mall", "distance": 3.2, "distanceUnit": "KM", "travelTime": 12, "travelTimeUnit": "MIN" }
        ]
      }
    },
    "roomConfiguration": {
      "roomTypes": [
        { "name": "Deluxe", "occupancy": 2 },
        { "name": "Deluxe Twin", "occupancy": 2 }
      ],
      "floors": [
        { "floorName": "1", "roomTypeName": "Deluxe", "roomCount": 5, "startNumber": 101 }
      ]
    },
    "finance": {
      "chartOfAccounts": [
        { "accountCode": "REV_ROOM", "accountName": "Room Revenue", "accountType": "REVENUE", "ledgerType": "LIABILITY", "active": true }
      ],
      "revenueMappings": [
        { "chargeType": "Room Charges", "mapGlAccount": "REV_ROOM", "status": "MAPPED", "description": "Room Booking Revenue" }
      ]
    },
    "payments": {
      "methods": [
        { "paymentMethod": "CASH", "accountMapping": "CASH_LEDGER", "allowRefund": true, "active": true }
      ]
    },
    "taxes": {
      "rules": [
        { "taxName": "GST", "type": "PERCENTAGE", "rate": 18.0, "applicableOn": "ADD_ON", "inclExcl": "EXCLUSIVE", "effectiveDate": "2026-06-01", "active": true, "status": "ACTIVE", "priority": 1 }
      ]
    }
  },
  "currentStep": "ROOM_CONFIGURATION",
  "completedSteps": ["PROPERTY_DETAILS", "CONTENT"]
}
```


