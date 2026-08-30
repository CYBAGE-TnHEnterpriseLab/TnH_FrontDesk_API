package com.pms.property.publish.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.pms.property.common.exception.BadRequestException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PublishValidator {

    public void validate(JsonNode root) {
        validatePropertyDetails(root.path("propertyDetails"));
        validateRoomConfiguration(root.path("roomConfiguration"));
        validateFinance(root.path("finance"));
        validatePayments(root.path("payments"));
        validateTaxes(root.path("taxes"));
    }

    private void validatePropertyDetails(JsonNode propertyDetails) {
        require(propertyDetails, "name");
        require(propertyDetails, "email");
        require(propertyDetails, "address");
        require(propertyDetails, "city");
        require(propertyDetails, "country");
        require(propertyDetails, "contactName");
        require(propertyDetails, "contactNumber");
        require(propertyDetails, "timeZone");
        require(propertyDetails, "nightAuditTime");
        require(propertyDetails, "checkInTime");
        require(propertyDetails, "checkOutTime");

        if (propertyDetails.path("address").asText().trim().length() < 10) {
            throw new BadRequestException("Address must have at least 10 characters");
        }

        String checkIn = propertyDetails.path("checkInTime").asText();
        String checkOut = propertyDetails.path("checkOutTime").asText();
        parseTime(checkIn, "checkInTime");
        parseTime(checkOut, "checkOutTime");
        parseTime(propertyDetails.path("nightAuditTime").asText(), "nightAuditTime");
        if (checkIn.equals(checkOut)) {
            throw new BadRequestException("checkOutTime must differ from checkInTime");
        }
    }

    private void validateRoomConfiguration(JsonNode roomConfiguration) {
        JsonNode roomTypes = roomConfiguration.path("roomTypes");
        JsonNode floors = roomConfiguration.path("floors");
        if (!roomTypes.isArray() || roomTypes.isEmpty()) {
            throw new BadRequestException("At least one room type is required");
        }
        if (!floors.isArray() || floors.isEmpty()) {
            throw new BadRequestException("At least one floor configuration is required");
        }

        Set<String> roomTypeNames = new HashSet<>();
        Set<String> masterNames = new HashSet<>();
        for (JsonNode roomType : roomTypes) {
            String name = roomType.path("name").asText();
            if (name.isBlank() || !roomTypeNames.add(name)) {
                throw new BadRequestException("Room type name must be unique and non-empty");
            }

            boolean isMaster = roomType.path("isMaster").asBoolean();
            String masterRoomName = roomType.path("masterRoomName").asText();
            if (isMaster) {
                masterNames.add(name);
                if (!masterRoomName.isBlank()) {
                    throw new BadRequestException("Master room cannot map to another master room");
                }
            } else if (masterRoomName.isBlank()) {
                throw new BadRequestException("Non-master room must map to a master room");
            }
        }

        for (JsonNode roomType : roomTypes) {
            if (!roomType.path("isMaster").asBoolean()) {
                String masterRoomName = roomType.path("masterRoomName").asText();
                if (!masterNames.contains(masterRoomName)) {
                    throw new BadRequestException("Mapped master room does not exist: " + masterRoomName);
                }
            }
        }

        Set<String> roomNumbers = new HashSet<>();
        for (JsonNode floor : floors) {
            String roomTypeName = floor.path("roomTypeName").asText();
            if (!roomTypeNames.contains(roomTypeName)) {
                throw new BadRequestException("Floor mapped to unknown room type: " + roomTypeName);
            }

            int roomCount = floor.path("roomCount").asInt();
            if (roomCount <= 0) {
                throw new BadRequestException("roomCount must be greater than zero");
            }

            JsonNode explicitRooms = floor.path("roomNumbers");
            if (explicitRooms.isArray() && !explicitRooms.isEmpty()) {
                for (JsonNode roomNumberNode : explicitRooms) {
                    String roomNumber = roomNumberNode.asText();
                    if (roomNumber.isBlank() || !roomNumbers.add(roomNumber)) {
                        throw new BadRequestException("Duplicate or blank room number found");
                    }
                }
            }
        }
    }

    private void validateFinance(JsonNode finance) {
        JsonNode chartOfAccounts = finance.path("chartOfAccounts");
        JsonNode revenueMappings = finance.path("revenueMappings");
        if (!chartOfAccounts.isArray() || chartOfAccounts.isEmpty()) {
            throw new BadRequestException("At least one chart of account entry is required");
        }
        if (!revenueMappings.isArray() || revenueMappings.isEmpty()) {
            throw new BadRequestException("At least one revenue mapping is required");
        }

        Set<String> ledgerCodes = new HashSet<>();
        for (JsonNode account : chartOfAccounts) {
            String ledgerCode = account.path("ledgerCode").asText();
            if (ledgerCode.isBlank()) {
                throw new BadRequestException("ledgerCode is required in chartOfAccounts");
            }
            ledgerCodes.add(ledgerCode);
        }

        for (JsonNode mapping : revenueMappings) {
            String ledgerCode = mapping.path("ledgerCode").asText();
            if (!ledgerCodes.contains(ledgerCode)) {
                throw new BadRequestException("Revenue mapping refers to unknown ledger: " + ledgerCode);
            }
        }
    }

    private void validatePayments(JsonNode payments) {
        JsonNode methods = payments.path("methods");
        if (!methods.isArray() || methods.isEmpty()) {
            throw new BadRequestException("At least one payment method is required");
        }
        require(payments.path("gateway"), "providerName");
        require(payments.path("gateway"), "merchantId");
        require(payments.path("gateway"), "apiKey");
        require(payments.path("gateway"), "secret");
        require(payments.path("gateway"), "mode");
    }

    private void validateTaxes(JsonNode taxes) {
        JsonNode rules = taxes.path("rules");
        if (!rules.isArray() || rules.isEmpty()) {
            throw new BadRequestException("At least one tax rule is required");
        }
        for (JsonNode rule : rules) {
            require(rule, "name");
            require(rule, "type");
            require(rule, "calculationType");
            if (rule.path("value").asDouble() <= 0) {
                throw new BadRequestException("Tax value must be greater than zero");
            }
        }
    }

    private void require(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || (value.isTextual() && value.asText().isBlank())) {
            throw new BadRequestException("Missing required field: " + field);
        }
    }

    private void parseTime(String value, String fieldName) {
        try {
            LocalTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid HH:mm time format for: " + fieldName);
        }
    }
}
