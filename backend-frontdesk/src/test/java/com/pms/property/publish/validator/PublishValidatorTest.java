package com.pms.property.publish.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.property.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class PublishValidatorTest {

    private final PublishValidator validator = new PublishValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldAcceptUiFinancePaymentTaxFields() throws Exception {
        assertDoesNotThrow(() -> validator.validate(objectMapper.readTree(validPayload())));
    }

    @Test
    void shouldRejectMissingAccountCode() {
        String payload = validPayload().replace("\"accountCode\": \"REV_ROOM\",\n", "");
        assertThrows(BadRequestException.class, () -> validator.validate(objectMapper.readTree(payload)));
    }

    @Test
    void shouldRejectUnknownMappedRevenueAccount() {
        String payload = validPayload().replace("\"mapGlAccount\": \"REV_ROOM\"", "\"mapGlAccount\": \"UNKNOWN\"");
        assertThrows(BadRequestException.class, () -> validator.validate(objectMapper.readTree(payload)));
    }

    @Test
    void shouldAcceptPendingRevenueMappingWithUnassignedAccount() throws Exception {
        String payload = validPayload()
            .replace("\"mapGlAccount\": \"REV_ROOM\"", "\"mapGlAccount\": \"UNASSIGNED\"")
            .replace("\"status\": \"MAPPED\"", "\"status\": \"PENDING\"");
        assertDoesNotThrow(() -> validator.validate(objectMapper.readTree(payload)));
    }

    @Test
    void shouldRejectMissingPaymentMethodRequiredFields() {
        String payload = validPayload().replace("\"allowRefund\": true,\n", "");
        assertThrows(BadRequestException.class, () -> validator.validate(objectMapper.readTree(payload)));
    }

    @Test
    void shouldRejectMissingTaxRate() {
        String payload = validPayload().replace("\"rate\": 18.0,\n", "");
        assertThrows(BadRequestException.class, () -> validator.validate(objectMapper.readTree(payload)));
    }

    @Test
    void shouldAcceptWithoutRoomConfigurationRoomTypes() throws Exception {
        String payload = validPayload().replace(
            """
                "roomTypes": [
                  {"name": "Standard Deluxe", "occupancy": 2}
                ],
            """,
            ""
        );
        assertDoesNotThrow(() -> validator.validate(objectMapper.readTree(payload)));
    }

    @Test
    void shouldAcceptFloorConfigurationAlias() throws Exception {
        String payload = validPayload().replace("\"roomConfiguration\"", "\"floorConfiguration\"");
        assertDoesNotThrow(() -> validator.validate(objectMapper.readTree(payload)));
    }

    private String validPayload() {
        return """
            {
              "propertyDetails": {
                "propertyName": "Seaside Retreat",
                "propertyType": "HOTEL",
                "propertyCode": "P-1001",
                "totalNoOfRooms": 2,
                "totalNoOfFloors": 1,
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
                "roomTypes": [
                  {"roomName": "Standard Deluxe", "quantity": 2, "maximumGuestOccupancy": 2}
                ]
              },
              "roomConfiguration": {
                "roomTypes": [
                  {"name": "Standard Deluxe", "occupancy": 2}
                ],
                "floors": [
                  {
                    "floorName": "2",
                    "roomTypes": [
                      {"roomTypeName": "Standard Deluxe", "roomNumbers": ["201", "202"]}
                    ]
                  }
                ]
              },
              "finance": {
                "chartOfAccounts": [
                  {
                    "accountCode": "REV_ROOM",
                    "accountName": "Room Revenue",
                    "accountType": "REVENUE",
                    "ledgerType": "LIABILITY",
                    "active": true
                  }
                ],
                "revenueMappings": [
                  {
                    "chargeType": "Room Charges",
                    "mapGlAccount": "REV_ROOM",
                    "status": "MAPPED",
                    "description": "Room Booking Revenue"
                  }
                ]
              },
              "payments": {
                "methods": [
                  {
                    "paymentMethod": "CASH",
                    "accountMapping": "CASH_LEDGER",
                    "allowRefund": true,
                    "active": true
                  }
                ]
              },
              "taxes": {
                "rules": [
                  {
                    "taxName": "GST",
                    "type": "PERCENTAGE",
                    "rate": 18.0,
                    "applicableOn": "ADD_ON",
                    "inclExcl": "EXCLUSIVE",
                    "effectiveDate": "2026-06-01",
                    "active": true,
                    "status": "ACTIVE",
                    "priority": 1
                  }
                ]
              }
            }
            """;
    }
}
