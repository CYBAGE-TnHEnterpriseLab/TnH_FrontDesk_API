package com.frontdesk.pms.rate_management.mapper;

import com.frontdesk.pms.rate_management.dto.*;
import com.frontdesk.pms.rate_management.entity.*;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MasterRoomMapper {
    public MasterRoom toEntity(MasterRoomRequestDTO dto) {
        MasterRoom entity = new MasterRoom();
        entity.setName(dto.getName());
        entity.setMealOption(dto.getMealOption());
        entity.setInclusion(dto.getInclusion());

        // Map pricingList
        if (dto.getPricingList() != null && !dto.getPricingList().isEmpty()) {
            List<MasterRoomPricing> pricingEntities = dto.getPricingList().stream().map(pricingDTO -> {
                MasterRoomPricing pricing = new MasterRoomPricing();
                pricing.setOccupancyType(pricingDTO.getOccupancyType());
                pricing.setPrice(pricingDTO.getPrice());
                pricing.setMasterRoom(entity); // set the back-reference
                return pricing;
            }).collect(Collectors.toList());
            entity.setPricingList(pricingEntities);
        }

        // Optionally map roomTypeMappings if needed (not implemented here)
        return entity;
    }

    public MasterRoomResponseDTO toResponseDTO(MasterRoom entity) {
        MasterRoomResponseDTO dto = new MasterRoomResponseDTO();
        dto.setId(entity.getId());
        dto.setPropertyId(entity.getPropertyId());
        dto.setName(entity.getName());
        dto.setMealOption(entity.getMealOption());
        dto.setInclusion(entity.getInclusion());
        if (entity.getPricingList() != null) {
            dto.setPricingList(entity.getPricingList().stream().map(this::toPricingResponseDTO).collect(Collectors.toList()));
        }
        if (entity.getRoomTypeMappings() != null) {
            dto.setRoomTypeMappings(entity.getRoomTypeMappings().stream().map(this::toMappingResponseDTO).collect(Collectors.toList()));
        }
        return dto;
    }

    public MasterRoomPricingResponseDTO toPricingResponseDTO(MasterRoomPricing entity) {
        MasterRoomPricingResponseDTO dto = new MasterRoomPricingResponseDTO();
        dto.setOccupancyType(entity.getOccupancyType());
        dto.setPrice(entity.getPrice());
        return dto;
    }

    public MasterRoomRoomTypeMappingResponseDTO toMappingResponseDTO(MasterRoomRoomTypeMapping entity) {
        MasterRoomRoomTypeMappingResponseDTO dto = new MasterRoomRoomTypeMappingResponseDTO();
        dto.setId(entity.getId());
        dto.setRoomTypeId(entity.getRoomTypeId());
        return dto;
    }
}
