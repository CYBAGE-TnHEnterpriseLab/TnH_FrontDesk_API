package com.pms.property.publish.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PublishMapperTest {

    @Test
    void shouldMapDraftJsonToNormalizedData() {
        String json = """
            {
              "propertyDetails": {
                "name": "Test",
                "email": "hotel@test.com",
                "address": "123 Sample Street",
                "city": "City",
                "country": "IN",
                "contactName": "John",
                "contactNumber": "+911234567890",
                "timeZone": "Asia/Kolkata",
                "nightAuditTime": "02:00",
                "checkInTime": "13:00",
                "checkOutTime": "11:00"
              },
              "content": {
                "specialRequests": [{"code": "EXTRA_PILLOW", "enabled": true}],
                "amenities": [{"code": "POOL", "enabled": true}]
              },
              "roomConfiguration": {
                "roomTypes": [
                  {"name": "Deluxe", "isMaster": true, "occupancy": 2},
                  {"name": "Deluxe Twin", "isMaster": false, "masterRoomName": "Deluxe", "occupancy": 2}
                ],
                "floors": [
                  {"floorName": "1", "roomTypeName": "Deluxe", "roomCount": 2, "roomNumbers": ["101", "102"]}
                ]
              },
              "finance": {
                "chartOfAccounts": [{"accountCode": "REV_ROOM", "accountName": "Room Revenue", "accountType": "REVENUE", "ledgerType": "LIABILITY", "active": true}],
                "revenueMappings": [{"chargeType": "Room Charges", "mapGlAccount": "REV_ROOM", "status": "MAPPED", "description": "Room Booking Revenue"}]
              },
              "payments": {
                "methods": [{"paymentMethod": "CASH", "accountMapping": "CASH_LEDGER", "allowRefund": true, "active": true}]
              },
              "taxes": {
                "rules": [{"taxName": "GST", "type": "PERCENTAGE", "rate": 18.0, "applicableOn": "ADD_ON", "inclExcl": "EXCLUSIVE", "effectiveDate": "2026-06-01", "active": true, "status": "ACTIVE", "priority": 1}]
              }
            }
            """;

        PublishMapper mapper = new PublishMapper(new ObjectMapper());
        PublishMapper.NormalizedPublishData data = mapper.toNormalized(json);

        assertEquals("Test", data.property().getTitle());
        assertEquals(2, data.inventoryRooms().size());
        assertEquals(1, data.taxRules().size());
        assertEquals(1, data.chartOfAccounts().size());
    }

    @Test
    void shouldMapUpdatedPropertyDetailsFields() {
        String json = """
            {
              "propertyDetails": {
                "propertyName": "Seaside Retreat",
                "propertyType": "HOTEL",
                "propertyCode": "P-1001",
                "totalNoOfRooms": 40,
                "totalNoOfFloors": 4,
                "website": "www.seaside-retreat.com",
                "addressLine1": "123 Beach Road",
                "addressLine2": "Near Lighthouse",
                "city": "Panaji",
                "state": "Goa",
                "country": "India",
                "zipCode": "403001",
                "timeZone": "Asia/Kolkata",
                "primaryContactName": "Nikhil",
                "phoneNumber": "+919999999999",
                "checkInTime": "11:55 AM",
                "checkOutTime": "00:00",
                "nightAuditTime": "11:55 PM"
              },
              "content": {
                "specialRequests": [{"code": "EXTRA_PILLOW", "enabled": true}],
                "amenities": [{"code": "POOL", "enabled": true}]
              },
              "roomConfiguration": {
                "roomTypes": [{"name": "Deluxe", "isMaster": true, "occupancy": 2}],
                "floors": [{"floorName": "1", "roomTypeName": "Deluxe", "roomCount": 1, "startNumber": 101}]
              },
              "finance": {
                "chartOfAccounts": [{"accountCode": "REV_ROOM", "accountName": "Room Revenue", "accountType": "REVENUE", "ledgerType": "LIABILITY", "active": true}],
                "revenueMappings": [{"chargeType": "Room Charges", "mapGlAccount": "REV_ROOM", "status": "MAPPED", "description": "Room Booking Revenue"}]
              },
              "payments": {
                "methods": [{"paymentMethod": "CASH", "accountMapping": "CASH_LEDGER", "allowRefund": true, "active": true}]
              },
              "taxes": {
                "rules": [{"taxName": "GST", "type": "PERCENTAGE", "rate": 18.0, "applicableOn": "ADD_ON", "inclExcl": "EXCLUSIVE", "effectiveDate": "2026-06-01", "active": true, "status": "ACTIVE", "priority": 1}]
              }
            }
            """;

        PublishMapper mapper = new PublishMapper(new ObjectMapper());
        PublishMapper.NormalizedPublishData data = mapper.toNormalized(json);

        assertEquals("Seaside Retreat", data.property().getTitle());
        assertEquals("123 Beach Road, Near Lighthouse", data.property().getAddress());
        assertEquals("Nikhil", data.property().getContactName());
        assertEquals("+919999999999", data.property().getContactNumber());
    }

    @Test
    void shouldMapUiNamedContentSections() {
        String json = """
            {
              "propertyDetails": {
                "propertyName": "Seaside Retreat",
                "propertyType": "HOTEL",
                "propertyCode": "P-1001",
                "totalNoOfRooms": 40,
                "totalNoOfFloors": 4,
                "addressLine1": "123 Beach Road",
                "city": "Panaji",
                "state": "Goa",
                "country": "India",
                "zipCode": "403001",
                "timeZone": "Asia/Kolkata",
                "primaryContactName": "Nikhil",
                "phoneNumber": "+919999999999",
                "checkInTime": "11:55 AM",
                "checkOutTime": "00:00",
                "nightAuditTime": "11:55 PM"
              },
              "content": {
                "propertyOverview": {
                  "propertyHeroImage": "s3://property/hero.jpg",
                  "propertyDescription": "Well located business hotel"
                },
                "guestServicesAndAmenities": {
                  "standardAmenities": [{"code": "SWIMMING_POOL", "enabled": true}],
                  "paidServices": [{"code": "AIRPORT_PICKUP", "enabled": true}],
                  "onDemandServices": [{"code": "EARLY_CHECKIN", "enabled": false}]
                },
                "nearbyLocationAccessibility": {
                  "transportConnectivity": [
                    {"type": "AIRPORT", "name": "DEL Airport", "distance": 8.3, "distanceUnit": "KM", "travelTime": 40, "travelTimeUnit": "MIN"}
                  ],
                  "otherLandmarks": [
                    {"type": "SHOPPING", "name": "Pacific Mall", "distance": 3.1, "distanceUnit": "KM", "travelTime": 15, "travelTimeUnit": "MIN"}
                  ]
                }
              },
              "roomConfiguration": {
                "roomTypes": [{"name": "Deluxe", "isMaster": true, "occupancy": 2}],
                "floors": [{"floorName": "1", "roomTypeName": "Deluxe", "roomCount": 1, "startNumber": 101}]
              },
              "finance": {
                "chartOfAccounts": [{"accountCode": "REV_ROOM", "accountName": "Room Revenue", "accountType": "REVENUE", "ledgerType": "LIABILITY", "active": true}],
                "revenueMappings": [{"chargeType": "Room Charges", "mapGlAccount": "REV_ROOM", "status": "MAPPED", "description": "Room Booking Revenue"}]
              },
              "payments": {
                "methods": [{"paymentMethod": "CASH", "accountMapping": "CASH_LEDGER", "allowRefund": true, "active": true}]
              },
              "taxes": {
                "rules": [{"taxName": "GST", "type": "PERCENTAGE", "rate": 18.0, "applicableOn": "ADD_ON", "inclExcl": "EXCLUSIVE", "effectiveDate": "2026-06-01", "active": true, "status": "ACTIVE", "priority": 1}]
              }
            }
            """;

        PublishMapper mapper = new PublishMapper(new ObjectMapper());
        PublishMapper.NormalizedPublishData data = mapper.toNormalized(json);

        assertEquals("Well located business hotel", data.propertyOverview().getPropertyDescription());
        assertEquals(3, data.guestServiceAmenities().size());
        assertEquals(2, data.nearbyLocationAccessibility().size());
    }

    @Test
    void shouldMapRoomsAndOutletsSections() {
        String json = """
            {
              "propertyDetails": {
                "propertyName": "Seaside Retreat",
                "propertyType": "HOTEL",
                "propertyCode": "P-1001",
                "totalNoOfRooms": 40,
                "totalNoOfFloors": 4,
                "addressLine1": "123 Beach Road",
                "city": "Panaji",
                "state": "Goa",
                "country": "India",
                "zipCode": "403001",
                "timeZone": "Asia/Kolkata",
                "primaryContactName": "Nikhil",
                "phoneNumber": "+919999999999",
                "checkInTime": "11:55 AM",
                "checkOutTime": "00:00",
                "nightAuditTime": "11:55 PM"
              },
              "content": {
                "specialRequests": [{"code": "EXTRA_PILLOW", "enabled": true}],
                "amenities": [{"code": "POOL", "enabled": true}]
              },
              "roomsAndOutlets": {
                "propertyAreas": [
                  {
                    "areaName": "Lounge Area",
                    "quantity": 1,
                    "maximumCapacity": 40,
                    "description": "Main lounge",
                    "amenities": ["AC", "Wifi"],
                    "images": ["img-1", "img-2"]
                  }
                ],
                "roomTypes": [
                  {
                    "roomName": "Deluxe Twin",
                    "roomCode": "DTW",
                    "quantity": 10,
                    "availableForSell": true,
                    "maximumGuestOccupancy": 2,
                    "description": "Large twin room",
                    "amenities": ["TV", "Mini Bar"],
                    "images": ["room-1"]
                  }
                ]
              },
              "roomConfiguration": {
                "roomTypes": [{"name": "Deluxe", "isMaster": true, "occupancy": 2}],
                "floors": [{"floorName": "1", "roomTypeName": "Deluxe", "roomCount": 1, "startNumber": 101}]
              },
              "finance": {
                "chartOfAccounts": [{"accountCode": "REV_ROOM", "accountName": "Room Revenue", "accountType": "REVENUE", "ledgerType": "LIABILITY", "active": true}],
                "revenueMappings": [{"chargeType": "Room Charges", "mapGlAccount": "REV_ROOM", "status": "MAPPED", "description": "Room Booking Revenue"}]
              },
              "payments": {
                "methods": [{"paymentMethod": "CASH", "accountMapping": "CASH_LEDGER", "allowRefund": true, "active": true}]
              },
              "taxes": {
                "rules": [{"taxName": "GST", "type": "PERCENTAGE", "rate": 18.0, "applicableOn": "ADD_ON", "inclExcl": "EXCLUSIVE", "effectiveDate": "2026-06-01", "active": true, "status": "ACTIVE", "priority": 1}]
              }
            }
            """;

        PublishMapper mapper = new PublishMapper(new ObjectMapper());
        PublishMapper.NormalizedPublishData data = mapper.toNormalized(json);

        assertEquals(1, data.propertyAreas().size());
        assertEquals("Lounge Area", data.propertyAreas().get(0).getAreaName());
        assertEquals(1, data.roomOutletTypes().size());
        assertEquals("Deluxe Twin", data.roomOutletTypes().get(0).getRoomName());
        assertEquals("DTW", data.roomOutletTypes().get(0).getRoomCode());
    }

    @Test
    void shouldMapFloorConfigureNestedRoomAndPropertyAreaAssignments() {
        String json = """
            {
              "propertyDetails": {
                "propertyName": "Seaside Retreat",
                "propertyType": "HOTEL",
                "propertyCode": "P-1001",
                "totalNoOfRooms": 12,
                "totalNoOfFloors": 3,
                "addressLine1": "123 Beach Road",
                "city": "Panaji",
                "state": "Goa",
                "country": "India",
                "zipCode": "403001",
                "timeZone": "Asia/Kolkata",
                "primaryContactName": "Nikhil",
                "phoneNumber": "+919999999999",
                "checkInTime": "11:55 AM",
                "checkOutTime": "00:00",
                "nightAuditTime": "11:55 PM"
              },
              "roomsAndOutlets": {
                "propertyAreas": [
                  {"propertyAreaCode": "LOUNGE", "areaName": "Lounge Area", "quantity": 2, "maximumCapacity": 30}
                ],
                "roomTypes": [
                  {"roomTypeCode": "EXESU", "roomName": "Executive Suite", "quantity": 12, "maximumGuestOccupancy": 3}
                ]
              },
              "roomConfiguration": {
                "roomTypes": [
                  {"roomTypeCode": "EXESU", "name": "Executive Suite", "isMaster": true, "occupancy": 3}
                ],
                "floors": [
                  {
                    "floorName": "Floor 1",
                    "propertyAreas": [
                      {"propertyAreaCode": "LOUNGE", "quantity": 1}
                    ],
                    "roomTypes": [
                      {"roomTypeCode": "EXESU", "quantity": 2, "roomNumbers": ["101", "102"]}
                    ]
                  },
                  {
                    "floorName": "Floor 2",
                    "roomTypes": [
                      {"roomTypeCode": "EXESU", "quantity": 1, "roomNumbers": ["201"]}
                    ]
                  }
                ]
              },
              "finance": {
                "chartOfAccounts": [{"accountCode": "REV_ROOM", "accountName": "Room Revenue", "accountType": "REVENUE", "ledgerType": "LIABILITY", "active": true}],
                "revenueMappings": [{"chargeType": "Room Charges", "mapGlAccount": "REV_ROOM", "status": "MAPPED", "description": "Room Booking Revenue"}]
              },
              "payments": {
                "methods": [{"paymentMethod": "CASH", "accountMapping": "CASH_LEDGER", "allowRefund": true, "active": true}]
              },
              "taxes": {
                "rules": [{"taxName": "GST", "type": "PERCENTAGE", "rate": 18.0, "applicableOn": "ADD_ON", "inclExcl": "EXCLUSIVE", "effectiveDate": "2026-06-01", "active": true, "status": "ACTIVE", "priority": 1}]
              }
            }
            """;

        PublishMapper mapper = new PublishMapper(new ObjectMapper());
        PublishMapper.NormalizedPublishData data = mapper.toNormalized(json);

        assertEquals(2, data.floors().size());
        assertEquals(1, data.floorPropertyAreas().size());
        assertEquals(3, data.inventoryRooms().size());
        assertTrue(data.floors().stream().allMatch(floor -> "EXESU".equals(floor.getRoomTypeName())));
    }

    @Test
    void shouldMapChartOfAccountsUiFields() {
        String json = """
            {
              "propertyDetails": {
                "name": "Test",
                "email": "hotel@test.com",
                "address": "123 Sample Street",
                "city": "City",
                "country": "IN",
                "contactName": "John",
                "contactNumber": "+911234567890",
                "timeZone": "Asia/Kolkata",
                "nightAuditTime": "02:00",
                "checkInTime": "13:00",
                "checkOutTime": "11:00"
              },
              "content": {
                "specialRequests": [{"code": "EXTRA_PILLOW", "enabled": true}],
                "amenities": [{"code": "POOL", "enabled": true}]
              },
              "roomConfiguration": {
                "roomTypes": [{"name": "Deluxe", "occupancy": 2}],
                "floors": [{"floorName": "1", "roomTypeName": "Deluxe", "roomCount": 1, "roomNumbers": ["101"]}]
              },
              "finance": {
                "chartOfAccounts": [
                  {
                    "accountCode": "4006-CIT",
                    "accountName": "Corporate Income Tax",
                    "accountType": "TAXES",
                    "ledgerType": "LIABILITY",
                    "active": true
                  }
                ],
                "revenueMappings": [{"chargeType": "Room Charges", "mapGlAccount": "4006-CIT", "status": "MAPPED", "description": "Room Booking Revenue"}]
              },
              "payments": {
                "methods": [{"paymentMethod": "CASH", "accountMapping": "CASH_LEDGER", "allowRefund": true, "active": true}]
              },
              "taxes": {
                "rules": [{"taxName": "GST", "type": "PERCENTAGE", "rate": 18.0, "applicableOn": "ADD_ON", "inclExcl": "EXCLUSIVE", "effectiveDate": "2026-06-01", "active": true, "status": "ACTIVE", "priority": 1}]
              }
            }
            """;

        PublishMapper mapper = new PublishMapper(new ObjectMapper());
        PublishMapper.NormalizedPublishData data = mapper.toNormalized(json);

        assertEquals(1, data.chartOfAccounts().size());
        assertEquals("4006-CIT", data.chartOfAccounts().get(0).getAccountCode());
        assertEquals("Corporate Income Tax", data.chartOfAccounts().get(0).getAccountName());
        assertEquals("TAXES", data.chartOfAccounts().get(0).getAccountType());
        assertEquals("LIABILITY", data.chartOfAccounts().get(0).getLedgerType());
        assertTrue(data.chartOfAccounts().get(0).getActive());
    }

    @Test
    void shouldMapFloorConfigurationAlias() {
        String json = """
            {
              "propertyDetails": {
                "name": "Test",
                "email": "hotel@test.com",
                "address": "123 Sample Street",
                "city": "City",
                "state": "State",
                "country": "IN",
                "zipCode": "411001",
                "contactName": "John",
                "contactNumber": "+911234567890",
                "propertyCode": "P-100",
                "propertyType": "HOTEL",
                "totalNoOfRooms": 2,
                "totalNoOfFloors": 1,
                "timeZone": "Asia/Kolkata",
                "nightAuditTime": "02:00",
                "checkInTime": "13:00",
                "checkOutTime": "11:00"
              },
              "roomsAndOutlets": {
                "roomTypes": [
                  {"roomName": "Dormitory", "quantity": 2, "maximumGuestOccupancy": 1}
                ]
              },
              "floorConfiguration": {
                "floors": [
                  {
                    "floorName": "2",
                    "roomTypes": [
                      {"roomTypeName": "Dormitory", "quantity": 2, "roomNumbers": ["203", "206"]}
                    ]
                  }
                ]
              },
              "finance": {
                "chartOfAccounts": [{"accountCode": "REV_ROOM", "accountName": "Room Revenue", "accountType": "REVENUE", "ledgerType": "LIABILITY", "active": true}],
                "revenueMappings": [{"chargeType": "Room Charges", "mapGlAccount": "REV_ROOM", "status": "MAPPED", "description": "Room Booking Revenue"}]
              },
              "payments": {
                "methods": [{"paymentMethod": "CASH", "accountMapping": "CASH_LEDGER", "allowRefund": true, "active": true}]
              },
              "taxes": {
                "rules": [{"taxName": "GST", "type": "PERCENTAGE", "rate": 18.0, "applicableOn": "ADD_ON", "inclExcl": "EXCLUSIVE", "effectiveDate": "2026-06-01", "active": true, "status": "ACTIVE", "priority": 1}]
              }
            }
            """;

        PublishMapper mapper = new PublishMapper(new ObjectMapper());
        PublishMapper.NormalizedPublishData data = mapper.toNormalized(json);

        assertEquals(2, data.inventoryRooms().size());
        assertEquals(1, data.floors().size());
    }
}


