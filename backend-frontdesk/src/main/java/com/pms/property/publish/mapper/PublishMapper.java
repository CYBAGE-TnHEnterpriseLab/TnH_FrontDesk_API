package com.pms.property.publish.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.property.common.exception.BadRequestException;
import com.pms.property.domain.content.AmenityEntity;
import com.pms.property.domain.content.SpecialRequestEntity;
import com.pms.property.domain.finance.ChartOfAccountEntity;
import com.pms.property.domain.finance.RevenueMappingEntity;
import com.pms.property.domain.payment.PaymentGatewayConfigEntity;
import com.pms.property.domain.payment.PaymentMethodEntity;
import com.pms.property.domain.property.PropertyEntity;
import com.pms.property.domain.room.FloorConfigurationEntity;
import com.pms.property.domain.room.InventoryRoomEntity;
import com.pms.property.domain.room.RoomTypeEntity;
import com.pms.property.domain.tax.TaxRuleEntity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PublishMapper {

    private final ObjectMapper objectMapper;

    public PublishMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NormalizedPublishData toNormalized(String wizardJson) {
        try {
            JsonNode root = objectMapper.readTree(wizardJson);
            JsonNode propertyNode = root.path("propertyDetails");

            PropertyEntity property = new PropertyEntity();
            property.setTitle(propertyNode.path("name").asText());
            property.setAddress(propertyNode.path("address").asText());
            property.setCity(propertyNode.path("city").asText());
            property.setCountry(propertyNode.path("country").asText());
            property.setEmail(propertyNode.path("email").asText());
            property.setContactName(propertyNode.path("contactName").asText());
            property.setContactNumber(propertyNode.path("contactNumber").asText());
            property.setTimeZone(propertyNode.path("timeZone").asText());
            property.setNightAuditTime(propertyNode.path("nightAuditTime").asText());
            property.setCheckInTime(propertyNode.path("checkInTime").asText());
            property.setCheckOutTime(propertyNode.path("checkOutTime").asText());
            property.setStatus("ACTIVE");
            property.setCreatedAt(Instant.now());

            List<SpecialRequestEntity> specialRequests = new ArrayList<>();
            for (JsonNode requestNode : root.path("content").path("specialRequests")) {
                SpecialRequestEntity request = new SpecialRequestEntity();
                request.setRequestCode(requestNode.path("code").asText());
                request.setEnabled(requestNode.path("enabled").asBoolean());
                specialRequests.add(request);
            }

            List<AmenityEntity> amenities = new ArrayList<>();
            for (JsonNode amenityNode : root.path("content").path("amenities")) {
                AmenityEntity amenity = new AmenityEntity();
                amenity.setAmenityCode(amenityNode.path("code").asText());
                amenity.setEnabled(amenityNode.path("enabled").asBoolean());
                amenities.add(amenity);
            }

            List<RoomTypeEntity> roomTypes = new ArrayList<>();
            for (JsonNode roomTypeNode : root.path("roomConfiguration").path("roomTypes")) {
                RoomTypeEntity roomType = new RoomTypeEntity();
                roomType.setName(roomTypeNode.path("name").asText());
                roomType.setMaster(roomTypeNode.path("isMaster").asBoolean());
                roomType.setMasterRoomName(roomTypeNode.path("masterRoomName").asText(null));
                roomType.setOccupancy(roomTypeNode.path("occupancy").asInt());
                roomTypes.add(roomType);
            }

            List<FloorConfigurationEntity> floors = new ArrayList<>();
            List<InventoryRoomEntity> inventoryRooms = new ArrayList<>();
            for (JsonNode floorNode : root.path("roomConfiguration").path("floors")) {
                FloorConfigurationEntity floor = new FloorConfigurationEntity();
                floor.setFloorName(floorNode.path("floorName").asText());
                floor.setRoomTypeName(floorNode.path("roomTypeName").asText());
                floor.setRoomCount(floorNode.path("roomCount").asInt());
                floor.setStartNumber(floorNode.path("startNumber").asInt());
                floors.add(floor);

                JsonNode manualRoomNumbers = floorNode.path("roomNumbers");
                if (manualRoomNumbers.isArray() && !manualRoomNumbers.isEmpty()) {
                    for (JsonNode roomNumberNode : manualRoomNumbers) {
                        InventoryRoomEntity room = new InventoryRoomEntity();
                        room.setFloorName(floor.getFloorName());
                        room.setRoomTypeName(floor.getRoomTypeName());
                        room.setRoomNumber(roomNumberNode.asText());
                        inventoryRooms.add(room);
                    }
                } else {
                    int start = floor.getStartNumber();
                    for (int i = 0; i < floor.getRoomCount(); i++) {
                        InventoryRoomEntity room = new InventoryRoomEntity();
                        room.setFloorName(floor.getFloorName());
                        room.setRoomTypeName(floor.getRoomTypeName());
                        room.setRoomNumber(String.valueOf(start + i));
                        inventoryRooms.add(room);
                    }
                }
            }

            List<ChartOfAccountEntity> chartOfAccounts = new ArrayList<>();
            for (JsonNode accountNode : root.path("finance").path("chartOfAccounts")) {
                ChartOfAccountEntity chartOfAccount = new ChartOfAccountEntity();
                chartOfAccount.setLedgerCode(accountNode.path("ledgerCode").asText());
                chartOfAccount.setLedgerName(accountNode.path("ledgerName").asText());
                chartOfAccount.setCategory(accountNode.path("category").asText());
                chartOfAccounts.add(chartOfAccount);
            }

            List<RevenueMappingEntity> revenueMappings = new ArrayList<>();
            for (JsonNode mappingNode : root.path("finance").path("revenueMappings")) {
                RevenueMappingEntity mapping = new RevenueMappingEntity();
                mapping.setPmsItem(mappingNode.path("pmsItem").asText());
                mapping.setLedgerCode(mappingNode.path("ledgerCode").asText());
                revenueMappings.add(mapping);
            }

            List<PaymentMethodEntity> paymentMethods = new ArrayList<>();
            for (JsonNode methodNode : root.path("payments").path("methods")) {
                PaymentMethodEntity paymentMethod = new PaymentMethodEntity();
                paymentMethod.setMethodCode(methodNode.path("code").asText());
                paymentMethod.setLedgerCode(methodNode.path("ledgerCode").asText());
                paymentMethod.setOnlineEnabled(methodNode.path("onlineEnabled").asBoolean());
                paymentMethods.add(paymentMethod);
            }

            PaymentGatewayConfigEntity gateway = new PaymentGatewayConfigEntity();
            JsonNode gatewayNode = root.path("payments").path("gateway");
            gateway.setProviderName(gatewayNode.path("providerName").asText());
            gateway.setMerchantId(gatewayNode.path("merchantId").asText());
            gateway.setApiKeyEncrypted(encrypt(gatewayNode.path("apiKey").asText()));
            gateway.setSecretEncrypted(encrypt(gatewayNode.path("secret").asText()));
            gateway.setMode(gatewayNode.path("mode").asText());
            gateway.setAutoCapture(gatewayNode.path("autoCapture").asBoolean());
            gateway.setThreeDsEnabled(gatewayNode.path("threeDsEnabled").asBoolean());

            List<TaxRuleEntity> taxRules = new ArrayList<>();
            for (JsonNode taxRuleNode : root.path("taxes").path("rules")) {
                TaxRuleEntity rule = new TaxRuleEntity();
                rule.setTaxName(taxRuleNode.path("name").asText());
                rule.setTaxType(taxRuleNode.path("type").asText());
                rule.setCalculationType(taxRuleNode.path("calculationType").asText());
                rule.setValue(taxRuleNode.path("value").asDouble());
                rule.setAppliesPerNight(taxRuleNode.path("appliesPerNight").asBoolean());
                rule.setPriority(taxRuleNode.path("priority").asInt());
                taxRules.add(rule);
            }

            return new NormalizedPublishData(
                root,
                property,
                specialRequests,
                amenities,
                roomTypes,
                floors,
                inventoryRooms,
                chartOfAccounts,
                revenueMappings,
                paymentMethods,
                gateway,
                taxRules
            );
        } catch (IOException ex) {
            throw new BadRequestException("Draft JSON is not valid");
        }
    }

    private String encrypt(String rawValue) {
        return Base64.getEncoder().encodeToString(rawValue.getBytes(StandardCharsets.UTF_8));
    }

    public record NormalizedPublishData(
        JsonNode root,
        PropertyEntity property,
        List<SpecialRequestEntity> specialRequests,
        List<AmenityEntity> amenities,
        List<RoomTypeEntity> roomTypes,
        List<FloorConfigurationEntity> floors,
        List<InventoryRoomEntity> inventoryRooms,
        List<ChartOfAccountEntity> chartOfAccounts,
        List<RevenueMappingEntity> revenueMappings,
        List<PaymentMethodEntity> paymentMethods,
        PaymentGatewayConfigEntity gateway,
        List<TaxRuleEntity> taxRules
    ) {
    }
}


