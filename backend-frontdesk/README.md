# Property Service (Draft JSON -> Normalized Publish)

Backend MVP for a 7-step Property Wizard.

## What this implements
- Draft data stored as JSON in `property_draft`.
- Publish converts draft JSON into normalized tables across:
  - property profile and operating timings
  - content (amenities/special requests)
  - room setup (room types, floors, generated inventory)
  - finance mappings (chart of accounts, revenue mappings)
  - payment methods and gateway config
  - tax rules
- APIs:
  - `POST /api/property/drafts`
  - `PUT /api/property/drafts/{draftId}`
  - `GET /api/property/drafts/{draftId}`
  - `POST /api/property/drafts/{draftId}/publish`

## Quick start
```powershell
mvn clean test
mvn spring-boot:run
```

## Sample payload for create/update draft
```json
{
  "schemaVersion": 1,
  "wizardData": {
    "propertyDetails": {
      "name": "Sea View Villa",
      "email": "ops@seaview.com",
      "address": "Beach Road, Dona Paula, Goa",
      "city": "Panaji",
      "country": "India",
      "contactName": "Nikhil",
      "contactNumber": "+919999999999",
      "timeZone": "Asia/Kolkata",
      "nightAuditTime": "02:00",
      "checkInTime": "13:00",
      "checkOutTime": "11:00"
    },
    "content": {
      "specialRequests": [
        { "code": "EXTRA_PILLOW", "enabled": true }
      ],
      "amenities": [
        { "code": "POOL", "enabled": true }
      ]
    },
    "roomConfiguration": {
      "roomTypes": [
        { "name": "Deluxe", "isMaster": true, "occupancy": 2 },
        { "name": "Deluxe Twin", "isMaster": false, "masterRoomName": "Deluxe", "occupancy": 2 }
      ],
      "floors": [
        { "floorName": "1", "roomTypeName": "Deluxe", "roomCount": 5, "startNumber": 101 }
      ]
    },
    "finance": {
      "chartOfAccounts": [
        { "ledgerCode": "REV_ROOM", "ledgerName": "Room Revenue", "category": "REVENUE" }
      ],
      "revenueMappings": [
        { "pmsItem": "ROOM_CHARGE", "ledgerCode": "REV_ROOM" }
      ]
    },
    "payments": {
      "methods": [
        { "code": "CASH", "ledgerCode": "CASH_LEDGER", "onlineEnabled": false }
      ],
      "gateway": {
        "providerName": "Razorpay",
        "merchantId": "merchant-1",
        "apiKey": "api-key",
        "secret": "secret-key",
        "mode": "TEST",
        "autoCapture": true,
        "threeDsEnabled": true
      }
    },
    "taxes": {
      "rules": [
        { "name": "GST", "type": "GST", "calculationType": "PERCENTAGE", "value": 18.0, "appliesPerNight": true, "priority": 1 }
      ]
    }
  },
  "currentStep": "ROOM_CONFIGURATION",
  "completedSteps": ["PROPERTY_DETAILS", "CONTENT"]
}
```


