            
        
    
package com.frontdesk.pms.rate_management.service;

import com.frontdesk.pms.rate_management.dto.MasterRoomRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomResponseDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomPricingRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomPricingResponseDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomRoomTypeMappingRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomRoomTypeMappingResponseDTO;
import com.frontdesk.pms.rate_management.dto.PropertyRoomTypeMappingResponseDTO;
import com.frontdesk.pms.rate_management.dto.RoomDTO;
import com.frontdesk.pms.rate_management.entity.MasterRoom;
import com.frontdesk.pms.rate_management.entity.MasterRoomPricing;
import com.frontdesk.pms.rate_management.entity.MasterRoomRoomTypeMapping;
import com.frontdesk.pms.rate_management.enums.OccupancyType;
import com.frontdesk.pms.rate_management.exception.MasterRoomNotFoundException;
import com.frontdesk.pms.rate_management.exception.PropertyNotFoundException;
import com.frontdesk.pms.rate_management.mapper.MasterRoomMapper;
import com.frontdesk.pms.rate_management.repository.MasterRoomRepository;
import com.frontdesk.pms.rate_management.repository.MasterRoomPricingRepository;
import com.frontdesk.pms.rate_management.repository.MasterRoomRoomTypeMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDate;

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
    @Autowired
    private PropertyWizardClient propertyWizardClient;

    @Transactional
    public MasterRoomResponseDTO createMasterRoom(String propertyId, MasterRoomRequestDTO masterRoomRequestDTO) {
        if (propertyId == null) {
            throw new IllegalArgumentException("propertyId is required in path");
        }
        if (!propertyWizardClient.propertyExists(propertyId)) {
            throw new PropertyNotFoundException(propertyId);
        }
        validateDateRange(masterRoomRequestDTO.getStartDate(), masterRoomRequestDTO.getEndDate());

        MasterRoom masterRoom = masterRoomMapper.toEntity(masterRoomRequestDTO);
        masterRoom.setPropertyId(propertyId);
        MasterRoom saved = masterRoomRepository.save(masterRoom);
        return masterRoomMapper.toResponseDTO(saved);
    }

    @Transactional
    public MasterRoomResponseDTO updateMasterRoom(String propertyId, Long id, MasterRoomRequestDTO masterRoomRequestDTO) {
        MasterRoom existing = getMasterRoomInProperty(propertyId, id);

        if (masterRoomRequestDTO.getName() != null) {
            existing.setName(masterRoomRequestDTO.getName());
        }
        if (masterRoomRequestDTO.getMealOption() != null) {
            existing.setMealOption(masterRoomRequestDTO.getMealOption());
        }
        if (masterRoomRequestDTO.getInclusion() != null) {
            existing.setInclusion(masterRoomRequestDTO.getInclusion());
        }
        validateDateRange(masterRoomRequestDTO.getStartDate(), masterRoomRequestDTO.getEndDate());
        if (masterRoomRequestDTO.getStartDate() != null) {
            existing.setStartDate(masterRoomRequestDTO.getStartDate());
        }
        if (masterRoomRequestDTO.getEndDate() != null) {
            existing.setEndDate(masterRoomRequestDTO.getEndDate());
        }

        if (masterRoomRequestDTO.getPricingList() != null) {
            syncMasterPricingList(existing, masterRoomRequestDTO.getPricingList());
        }

        MasterRoom saved = masterRoomRepository.save(existing);
        return masterRoomMapper.toResponseDTO(saved);
    }


    public Optional<MasterRoomResponseDTO> getMasterRoom(Long id) {
        return masterRoomRepository.findById(id).map(masterRoomMapper::toResponseDTO);
    }


    public List<MasterRoomResponseDTO> getAllMasterRooms() {
        return masterRoomRepository.findAll().stream()
                .sorted(Comparator.comparing(MasterRoom::getId, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(masterRoomMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<MasterRoomResponseDTO> getMasterRoomsByPropertyId(String propertyId) {
        return masterRoomRepository.findByPropertyId(propertyId)
                .stream()
                .sorted(Comparator.comparing(MasterRoom::getId, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(masterRoomMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
    }

    @Transactional
    public void deleteMasterRoom(String propertyId, Long id) {
        getMasterRoomInProperty(propertyId, id);
        masterRoomRepository.deleteById(id);
    }

    @Transactional
    public MasterRoomPricingResponseDTO addOrUpdatePricing(String propertyId, Long masterRoomId, MasterRoomPricingRequestDTO pricingRequestDTO) {
        MasterRoom masterRoom = getMasterRoomInProperty(propertyId, masterRoomId);
        String normalizedOccupancyType = OccupancyType.normalizeOrThrow(pricingRequestDTO.getOccupancyType());

        MasterRoomPricing pricing = masterRoomPricingRepository
            .findByMasterRoomIdAndRoomTypeIdIsNullAndOccupancyType(masterRoomId, normalizedOccupancyType)
            .orElseGet(MasterRoomPricing::new);

        pricing.setMasterRoom(masterRoom);
        pricing.setRoomTypeId(null);
        pricing.setInherited(false);
        pricing.setParentPricingId(null);
        pricing.setOccupancyType(normalizedOccupancyType);
        pricing.setPrice(pricingRequestDTO.getPrice());
        MasterRoomPricing saved = masterRoomPricingRepository.save(pricing);

        // Automatically update inherited pricing for all mapped room types
        updateInheritedPricingForMasterRoom(masterRoomId);

        return masterRoomMapper.toPricingResponseDTO(saved);
    }


    public List<MasterRoomPricingResponseDTO> getPricingByMasterRoom(Long masterRoomId) {
        return masterRoomPricingRepository.findByMasterRoomIdAndRoomTypeIdIsNull(masterRoomId).stream()
                .map(masterRoomMapper::toPricingResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MasterRoomRoomTypeMappingResponseDTO mapRoomType(String propertyId, Long masterRoomId, MasterRoomRoomTypeMappingRequestDTO mappingRequestDTO) {
        return upsertRoomTypeMapping(propertyId, mappingRequestDTO.getRoomTypeId(), masterRoomId);
    }

    @Transactional
    public MasterRoomRoomTypeMappingResponseDTO upsertRoomTypeMapping(String propertyId, Long roomTypeId, Long masterRoomId) {
        if (roomTypeId == null) {
            throw new IllegalArgumentException("roomTypeId is required");
        }
        if (masterRoomId == null) {
            throw new IllegalArgumentException("masterRoomId is required");
        }

        MasterRoom masterRoom = getMasterRoomInProperty(propertyId, masterRoomId);

        MasterRoomRoomTypeMapping mapping = mappingRepository
                .findByMasterRoomPropertyIdAndRoomTypeId(propertyId, roomTypeId)
                .orElseGet(MasterRoomRoomTypeMapping::new);

        mapping.setMasterRoom(masterRoom);
        mapping.setRoomTypeId(roomTypeId);
        MasterRoomRoomTypeMapping saved = mappingRepository.save(mapping);

        // Inherit all pricing from selected master room to this room type.
        // If a manual override exists for an occupancy, it is preserved.
        List<MasterRoomPricing> masterPricings = masterRoomPricingRepository.findByMasterRoomId(masterRoomId);
        for (MasterRoomPricing masterPricing : masterPricings) {
            upsertInheritedPricingForRoomType(masterPricing, roomTypeId);
        }

        return masterRoomMapper.toMappingResponseDTO(saved);
    }

    // Manual override: set a specific price for a room type and occupancy, breaking inheritance for that entry
        @Transactional
        public void overrideRoomTypePricing(Long roomTypeId, String occupancyType, Double newPrice) {
            String normalizedOccupancyType = OccupancyType.normalizeOrThrow(occupancyType);
            masterRoomPricingRepository.findByRoomTypeIdAndOccupancyType(roomTypeId, normalizedOccupancyType)
                    .ifPresent(p -> {
                        p.setPrice(newPrice);
                        p.setInherited(false);
                        masterRoomPricingRepository.save(p);
                    });
        }

            // Break inheritance for all pricing of a room type
            @Transactional
            public void breakInheritanceForRoomType(Long roomTypeId) {
                masterRoomPricingRepository.findByRoomTypeId(roomTypeId).stream()
                        .filter(p -> Boolean.TRUE.equals(p.getInherited()))
                        .forEach(p -> {
                            p.setInherited(false);
                            masterRoomPricingRepository.save(p);
                        });
            }

        // Update all inherited pricing for mapped room types when master pricing changes
        @Transactional
        public void updateInheritedPricingForMasterRoom(Long masterRoomId) {
            // Get all master pricing for this master room
            List<MasterRoomPricing> masterPricings = masterRoomPricingRepository.findByMasterRoomIdAndRoomTypeIdIsNull(masterRoomId);
            List<Long> mappedRoomTypeIds = mappingRepository.findByMasterRoomId(masterRoomId)
                    .stream()
                    .map(MasterRoomRoomTypeMapping::getRoomTypeId)
                    .toList();

            // For each master pricing, update inherited child pricing and create missing inherited rows.
            for (MasterRoomPricing masterPricing : masterPricings) {
                List<MasterRoomPricing> inheritedPricings = masterRoomPricingRepository
                        .findByInheritedTrueAndParentPricingId(masterPricing.getId());

                Map<Long, MasterRoomPricing> inheritedByRoomType = new HashMap<>();
                for (MasterRoomPricing inherited : inheritedPricings) {
                    if (inherited.getRoomTypeId() != null) {
                        inheritedByRoomType.put(inherited.getRoomTypeId(), inherited);
                    }
                }

                for (Long roomTypeId : mappedRoomTypeIds) {
                    MasterRoomPricing inherited = inheritedByRoomType.get(roomTypeId);
                    if (inherited != null) {
                        inherited.setPrice(masterPricing.getPrice());
                        masterRoomPricingRepository.save(inherited);
                        continue;
                    }

                    upsertInheritedPricingForRoomType(masterPricing, roomTypeId);
                }
            }
        }


    public List<MasterRoomRoomTypeMappingResponseDTO> getMappingsByMasterRoom(Long masterRoomId) {
        return mappingRepository.findAll().stream()
                .filter(m -> m.getMasterRoom().getId().equals(masterRoomId))
                .map(masterRoomMapper::toMappingResponseDTO)
                .collect(Collectors.toList());
    }

    public List<PropertyRoomTypeMappingResponseDTO> getMappingsByPropertyId(String propertyId) {
        List<MasterRoomRoomTypeMapping> mappings = mappingRepository.findByMasterRoomPropertyId(propertyId);
        Map<Long, MasterRoomRoomTypeMapping> mappingByRoomTypeId = mappings.stream()
                .collect(Collectors.toMap(MasterRoomRoomTypeMapping::getRoomTypeId, Function.identity(), (first, second) -> first));

        RoomDTO[] roomTypes = propertyWizardClient.getRoomTypesByProperty(propertyId);

        Set<Long> currentRoomTypeIds = roomTypes == null
            ? Set.of()
            : java.util.Arrays.stream(roomTypes)
            .map(RoomDTO::getId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));

        reconcileMappingsWithPropertyWizard(mappings, currentRoomTypeIds);

        if (roomTypes == null || roomTypes.length == 0) {
            return List.of();
        }

        List<PropertyRoomTypeMappingResponseDTO> response = java.util.Arrays.stream(roomTypes)
                .map(roomType -> {
                    MasterRoomRoomTypeMapping mapping = mappingByRoomTypeId.get(roomType.getId());

                    PropertyRoomTypeMappingResponseDTO dto = new PropertyRoomTypeMappingResponseDTO();
                    dto.setRoomTypeId(roomType.getId());
                    dto.setRoomTypeName(roomType.getName());
                    dto.setMapped(mapping != null);

                    if (mapping != null) {
                        dto.setMappingId(mapping.getId());
                        dto.setMasterRoomId(mapping.getMasterRoom().getId());
                        dto.setMasterRoomName(mapping.getMasterRoom().getName());
                    }

                    dto.setInheritedRates(masterRoomPricingRepository.findByRoomTypeId(roomType.getId()).stream()
                            .map(masterRoomMapper::toPricingResponseDTO)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
        return response;
    }

    private void reconcileMappingsWithPropertyWizard(List<MasterRoomRoomTypeMapping> mappings, Set<Long> currentRoomTypeIds) {
        if (mappings == null || mappings.isEmpty()) {
            return;
        }

        List<MasterRoomRoomTypeMapping> staleMappings = mappings.stream()
                .filter(mapping -> mapping.getRoomTypeId() == null || !currentRoomTypeIds.contains(mapping.getRoomTypeId()))
                .collect(Collectors.toList());

        for (MasterRoomRoomTypeMapping staleMapping : staleMappings) {
            if (staleMapping.getRoomTypeId() != null) {
                List<MasterRoomPricing> staleRoomTypePricing = masterRoomPricingRepository.findByRoomTypeId(staleMapping.getRoomTypeId());
                if (!staleRoomTypePricing.isEmpty()) {
                    masterRoomPricingRepository.deleteAll(staleRoomTypePricing);
                }
            }
        }

        if (!staleMappings.isEmpty()) {
            mappingRepository.deleteAll(staleMappings);
        }
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
        return masterRoomPricingRepository.findByRoomTypeId(roomTypeId).stream()
                .map(masterRoomMapper::toPricingResponseDTO)
                .collect(Collectors.toList());
    }

    private void upsertInheritedPricingForRoomType(MasterRoomPricing masterPricing, Long roomTypeId) {
        Optional<MasterRoomPricing> existingForRoomTypeAndOccupancy = masterRoomPricingRepository
                .findByRoomTypeIdAndOccupancyType(roomTypeId, masterPricing.getOccupancyType());

        if (existingForRoomTypeAndOccupancy.isPresent()) {
            MasterRoomPricing existing = existingForRoomTypeAndOccupancy.get();
            if (Boolean.TRUE.equals(existing.getInherited())) {
                    existing.setMasterRoom(masterPricing.getMasterRoom());
                existing.setParentPricingId(masterPricing.getId());
                existing.setPrice(masterPricing.getPrice());
                masterRoomPricingRepository.save(existing);
            }
            return;
        }

        MasterRoomPricing inheritedPricing = new MasterRoomPricing();
        inheritedPricing.setMasterRoom(masterPricing.getMasterRoom());
        inheritedPricing.setRoomTypeId(roomTypeId);
        inheritedPricing.setInherited(true);
        inheritedPricing.setParentPricingId(masterPricing.getId());
        inheritedPricing.setOccupancyType(masterPricing.getOccupancyType());
        inheritedPricing.setPrice(masterPricing.getPrice());
        // masterRoom is null for child pricing
        masterRoomPricingRepository.save(inheritedPricing);
    }

    private void syncMasterPricingList(MasterRoom masterRoom, List<MasterRoomPricingRequestDTO> pricingList) {
        Long masterRoomId = masterRoom.getId();
        List<MasterRoomPricing> existingMasterPricing = masterRoomPricingRepository.findByMasterRoomIdAndRoomTypeIdIsNull(masterRoomId);
        Map<String, MasterRoomPricing> existingByOccupancy = existingMasterPricing.stream()
                .collect(Collectors.toMap(
                        pricing -> OccupancyType.normalizeOrThrow(pricing.getOccupancyType()),
                        Function.identity(),
                        (first, second) -> first
                ));

        Set<String> requestedOccupancies = new LinkedHashSet<>();

        for (MasterRoomPricingRequestDTO pricingRequestDTO : pricingList) {
            String normalizedOccupancy = OccupancyType.normalizeOrThrow(pricingRequestDTO.getOccupancyType());
            requestedOccupancies.add(normalizedOccupancy);

            MasterRoomPricing pricing = existingByOccupancy.get(normalizedOccupancy);
            if (pricing == null) {
                pricing = new MasterRoomPricing();
            }

            pricing.setMasterRoom(masterRoom);
            pricing.setRoomTypeId(null);
            pricing.setInherited(false);
            pricing.setParentPricingId(null);
            pricing.setOccupancyType(normalizedOccupancy);
            pricing.setPrice(pricingRequestDTO.getPrice());
            masterRoomPricingRepository.save(pricing);
        }

        List<MasterRoomPricing> staleMasterPricing = existingMasterPricing.stream()
                .filter(pricing -> !requestedOccupancies.contains(OccupancyType.normalizeOrThrow(pricing.getOccupancyType())))
                .collect(Collectors.toList());

        if (!staleMasterPricing.isEmpty()) {
            masterRoomPricingRepository.deleteAll(staleMasterPricing);
        }

        updateInheritedPricingForMasterRoom(masterRoomId);
    }

    private MasterRoom getMasterRoomInProperty(String propertyId, Long masterRoomId) {
        if (propertyId == null) {
            throw new IllegalArgumentException("propertyId is required in path");
        }
        MasterRoom masterRoom = masterRoomRepository.findById(masterRoomId)
                .orElseThrow(() -> new MasterRoomNotFoundException(masterRoomId));
        if (!propertyId.equals(masterRoom.getPropertyId())) {
            throw new MasterRoomNotFoundException(masterRoomId);
        }
        return masterRoom;
    }
}
