package com.pms.property.publish.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                  {"floorName": "1", "roomTypeName": "Deluxe", "roomCount": 2, "startNumber": 101}
                ]
              },
              "finance": {
                "chartOfAccounts": [{"ledgerCode": "REV_ROOM", "ledgerName": "Room Revenue", "category": "REVENUE"}],
                "revenueMappings": [{"pmsItem": "ROOM_CHARGE", "ledgerCode": "REV_ROOM"}]
              },
              "payments": {
                "methods": [{"code": "CASH", "ledgerCode": "CASH_LEDGER", "onlineEnabled": false}],
                "gateway": {
                  "providerName": "Razorpay",
                  "merchantId": "M123",
                  "apiKey": "api-key",
                  "secret": "secret-key",
                  "mode": "TEST",
                  "autoCapture": true,
                  "threeDsEnabled": true
                }
              },
              "taxes": {
                "rules": [{"name": "GST", "type": "GST", "calculationType": "PERCENTAGE", "value": 18.0, "appliesPerNight": true, "priority": 1}]
              }
            }
            """;

        PublishMapper mapper = new PublishMapper(new ObjectMapper());
        PublishMapper.NormalizedPublishData data = mapper.toNormalized(json);

        assertEquals("Test", data.property().getTitle());
        assertEquals(2, data.roomTypes().size());
        assertEquals(2, data.inventoryRooms().size());
        assertEquals(1, data.taxRules().size());
        assertEquals(1, data.chartOfAccounts().size());
    }
}


