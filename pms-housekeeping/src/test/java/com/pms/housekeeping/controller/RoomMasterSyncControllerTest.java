package com.pms.housekeeping.controller;

import com.pms.housekeeping.dto.request.RoomMasterSyncRequest;
import com.pms.housekeeping.dto.response.RoomMasterSyncResponse;
import com.pms.housekeeping.service.RoomMasterSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomMasterSyncControllerTest {

    @Mock
    private RoomMasterSyncService roomMasterSyncService;

    private RoomMasterSyncController controller;

    @BeforeEach
    void setUp() {
        controller = new RoomMasterSyncController(roomMasterSyncService);
    }

    @Test
    void sync_shouldReturnCountsFromService() {
        String propertyId = UUID.randomUUID().toString();
        RoomMasterSyncRequest request = new RoomMasterSyncRequest(
                propertyId,
                LocalDate.of(2026, 8, 18),
                LocalDate.of(2026, 8, 20),
                List.of(new RoomMasterSyncRequest.RoomMasterUnit(
                        "13",
                        "Deluxe",
                        "101",
                        "1",
                        "North",
                        "CLASS-A",
                        "WiFi",
                        true,
                        true
                ))
        );
        RoomMasterSyncResponse result = new RoomMasterSyncResponse(7, 2);
        when(roomMasterSyncService.sync(request)).thenReturn(result);

        ResponseEntity<Map<String, Integer>> response = controller.sync(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("syncedRooms", 7).containsEntry("deactivatedRooms", 2);
        verify(roomMasterSyncService).sync(request);
    }
}

