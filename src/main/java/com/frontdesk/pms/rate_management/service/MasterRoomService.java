            
        
    
package com.frontdesk.pms.rate_management.service;

import com.frontdesk.pms.rate_management.dto.MasterRoomRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomResponseDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomPricingRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomPricingResponseDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomRoomTypeMappingRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomRoomTypeMappingResponseDTO;
import com.frontdesk.pms.rate_management.entity.MasterRoom;
import com.frontdesk.pms.rate_management.entity.MasterRoomPricing;
import com.frontdesk.pms.rate_management.entity.MasterRoomRoomTypeMapping;
import com.frontdesk.pms.rate_management.mapper.MasterRoomMapper;
import com.frontdesk.pms.rate_management.repository.MasterRoomRepository;
import com.frontdesk.pms.rate_management.repository.MasterRoomPricingRepository;
import com.frontdesk.pms.rate_management.repository.MasterRoomRoomTypeMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MasterRoomService {
    @Autowired
    private MasterRoomRepository masterRoomRepository;
    @Autowired
    private MasterRoomPricingRepository masterRoomPricingRepository;
    @Autowired
    private MasterRoomRoomTypeMappingRepository mappingRepository;
    @Autowired
    private MasterRoomMapper masterRoomMapper;

    @Transactional


    public MasterRoomResponseDTO createOrUpdateMasterRoom(MasterRoomRequestDTO masterRoomRequestDTO) {
        MasterRoom masterRoom = masterRoomMapper.toEntity(masterRoomRequestDTO);
        MasterRoom saved = masterRoomRepository.save(masterRoom);
        return masterRoomMapper.toResponseDTO(saved);
    }


    public Optional<MasterRoomResponseDTO> getMasterRoom(Long id) {
        return masterRoomRepository.findById(id).map(masterRoomMapper::toResponseDTO);
    }


    public List<MasterRoomResponseDTO> getAllMasterRooms() {
        return masterRoomRepository.findAll().stream().map(masterRoomMapper::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional
    public void deleteMasterRoom(Long id) {
        masterRoomRepository.deleteById(id);
    }

    @Transactional
    public MasterRoomPricingResponseDTO addOrUpdatePricing(Long masterRoomId, MasterRoomPricingRequestDTO pricingRequestDTO) {
        MasterRoom masterRoom = masterRoomRepository.findById(masterRoomId).orElseThrow();
        MasterRoomPricing pricing = new MasterRoomPricing();
        pricing.setMasterRoom(masterRoom);
        pricing.setOccupancyType(pricingRequestDTO.getOccupancyType());
        pricing.setPrice(pricingRequestDTO.getPrice());
        MasterRoomPricing saved = masterRoomPricingRepository.save(pricing);

        // Automatically update inherited pricing for all mapped room types
        updateInheritedPricingForMasterRoom(masterRoomId);

        return masterRoomMapper.toPricingResponseDTO(saved);
    }


    public List<MasterRoomPricingResponseDTO> getPricingByMasterRoom(Long masterRoomId) {
        return masterRoomPricingRepository.findAll().stream()
                .filter(p -> p.getMasterRoom().getId().equals(masterRoomId))
                .map(masterRoomMapper::toPricingResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MasterRoomRoomTypeMappingResponseDTO mapRoomType(Long masterRoomId, MasterRoomRoomTypeMappingRequestDTO mappingRequestDTO) {
        MasterRoom masterRoom = masterRoomRepository.findById(masterRoomId).orElseThrow();
        MasterRoomRoomTypeMapping mapping = new MasterRoomRoomTypeMapping();
        mapping.setMasterRoom(masterRoom);
        mapping.setRoomTypeId(mappingRequestDTO.getRoomTypeId());
        MasterRoomRoomTypeMapping saved = mappingRepository.save(mapping);

        // Inherit all pricing from master room to this room type
        Long roomTypeId = mappingRequestDTO.getRoomTypeId();
        masterRoomPricingRepository.findAll().stream()
            .filter(p -> p.getMasterRoom() != null && p.getMasterRoom().getId().equals(masterRoomId))
            .forEach(masterPricing -> {
                MasterRoomPricing inheritedPricing = new MasterRoomPricing();
                inheritedPricing.setRoomTypeId(roomTypeId);
                inheritedPricing.setInherited(true);
                inheritedPricing.setParentPricingId(masterPricing.getId());
                inheritedPricing.setOccupancyType(masterPricing.getOccupancyType());
                inheritedPricing.setPrice(masterPricing.getPrice());
                // masterRoom is null for child pricing
                masterRoomPricingRepository.save(inheritedPricing);
            });

        return masterRoomMapper.toMappingResponseDTO(saved);
    }

    // Manual override: set a specific price for a room type and occupancy, breaking inheritance for that entry
            @Transactional
            public void overrideRoomTypePricing(Long roomTypeId, String occupancyType, Double newPrice) {
                masterRoomPricingRepository.findAll().stream()
                    .filter(p -> p.getRoomTypeId() != null && p.getRoomTypeId().equals(roomTypeId)
                            && p.getOccupancyType().equals(occupancyType))
                    .forEach(p -> {
                        p.setPrice(newPrice);
                        p.setInherited(false);
                        masterRoomPricingRepository.save(p);
                    });
            }

            // Break inheritance for all pricing of a room type
            @Transactional
            public void breakInheritanceForRoomType(Long roomTypeId) {
                masterRoomPricingRepository.findAll().stream()
                    .filter(p -> p.getRoomTypeId() != null && p.getRoomTypeId().equals(roomTypeId) && Boolean.TRUE.equals(p.getInherited()))
                    .forEach(p -> {
                        p.setInherited(false);
                        masterRoomPricingRepository.save(p);
                    });
            }

        // Update all inherited pricing for mapped room types when master pricing changes
        @Transactional
        public void updateInheritedPricingForMasterRoom(Long masterRoomId) {
            // Get all master pricing for this master room
            List<MasterRoomPricing> masterPricings = masterRoomPricingRepository.findAll().stream()
                    .filter(p -> p.getMasterRoom() != null && p.getMasterRoom().getId().equals(masterRoomId))
                    .collect(Collectors.toList());

            // For each master pricing, update all inherited child pricing (only if still inherited)
            for (MasterRoomPricing masterPricing : masterPricings) {
                List<MasterRoomPricing> inheritedPricings = masterRoomPricingRepository.findAll().stream()
                        .filter(p -> Boolean.TRUE.equals(p.getInherited()) &&
                                p.getParentPricingId() != null &&
                                p.getParentPricingId().equals(masterPricing.getId()))
                        .collect(Collectors.toList());
                for (MasterRoomPricing inherited : inheritedPricings) {
                    // Only update if still inherited
                    if (Boolean.TRUE.equals(inherited.getInherited())) {
                        inherited.setPrice(masterPricing.getPrice());
                        masterRoomPricingRepository.save(inherited);
                    }
                }
            }
        }


    public List<MasterRoomRoomTypeMappingResponseDTO> getMappingsByMasterRoom(Long masterRoomId) {
        return mappingRepository.findAll().stream()
                .filter(m -> m.getMasterRoom().getId().equals(masterRoomId))
                .map(masterRoomMapper::toMappingResponseDTO)
                .collect(Collectors.toList());
    }

    public boolean isAllRoomTypesMapped(List<Long> activeRoomTypeIds) {
        List<Long> mappedRoomTypeIds = mappingRepository.findAll().stream()
                .map(MasterRoomRoomTypeMapping::getRoomTypeId)
                .toList();
        return activeRoomTypeIds.stream().allMatch(mappedRoomTypeIds::contains);
    }

    // Mapping logic is now handled by MasterRoomMapper

    // Fetch pricing for a specific room type (inherited or overridden)
    public List<MasterRoomPricingResponseDTO> getPricingByRoomType(Long roomTypeId) {
        return masterRoomPricingRepository.findAll().stream()
                .filter(p -> p.getRoomTypeId() != null && p.getRoomTypeId().equals(roomTypeId))
                .map(masterRoomMapper::toPricingResponseDTO)
                .collect(Collectors.toList());
    }
}
