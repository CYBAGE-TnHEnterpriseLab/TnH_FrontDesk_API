package com.pms.housekeeping.service.impl;

import com.pms.housekeeping.dto.HousekeepingRoomStatusRequestDto;
import com.pms.housekeeping.dto.HousekeepingRoomStatusResponseDto;
import com.pms.housekeeping.entity.HousekeepingRoomStatusRecord;
import com.pms.housekeeping.repository.HousekeepingRoomStatusRepository;
import com.pms.housekeeping.service.HousekeepingRoomStatusService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HousekeepingRoomStatusServiceImpl implements HousekeepingRoomStatusService {

    private final HousekeepingRoomStatusRepository housekeepingRoomStatusRepository;

    @Override
    public HousekeepingRoomStatusResponseDto markOccupied(HousekeepingRoomStatusRequestDto request) {
        return saveStatus(request, "OCCUPIED");
    }

    @Override
    public HousekeepingRoomStatusResponseDto markDirty(HousekeepingRoomStatusRequestDto request) {
        return saveStatus(request, "DIRTY");
    }

    @Override
    public HousekeepingRoomStatusResponseDto updateManualStatus(HousekeepingRoomStatusRequestDto request, String roomStatus) {
        return saveStatus(request, normalizeStatus(roomStatus));
    }

    private HousekeepingRoomStatusResponseDto saveStatus(HousekeepingRoomStatusRequestDto request, String roomStatus) {
        HousekeepingRoomStatusRecord record = housekeepingRoomStatusRepository
                .findByPropertyIdAndBusinessDateAndConfirmationNumber(
                        request.getPropertyId(),
                        request.getBusinessDate(),
                        request.getConfirmationNumber()
                )
                .orElseGet(HousekeepingRoomStatusRecord::new);

        record.setPropertyId(request.getPropertyId());
        record.setBusinessDate(request.getBusinessDate());
        record.setConfirmationNumber(request.getConfirmationNumber());
        if (StringUtils.hasText(request.getRoomNo())) {
            record.setRoomNo(request.getRoomNo().trim());
        }
        record.setRoomStatus(roomStatus);

        HousekeepingRoomStatusRecord saved = housekeepingRoomStatusRepository.save(record);
        return HousekeepingRoomStatusResponseDto.builder()
                .propertyId(saved.getPropertyId())
                .businessDate(saved.getBusinessDate())
                .confirmationNumber(saved.getConfirmationNumber())
                .roomNo(saved.getRoomNo())
                .roomStatus(saved.getRoomStatus())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    private String normalizeStatus(String roomStatus) {
        if (!StringUtils.hasText(roomStatus)) {
            return roomStatus;
        }
        return roomStatus.trim().toUpperCase(Locale.ROOT);
    }
}
