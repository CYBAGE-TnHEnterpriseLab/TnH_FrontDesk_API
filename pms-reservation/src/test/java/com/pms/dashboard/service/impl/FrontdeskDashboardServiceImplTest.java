package com.pms.dashboard.service.impl;

import com.pms.dashboard.client.HousekeepingDashboardClient;
import com.pms.dashboard.client.InventoryDashboardClient;
import com.pms.dashboard.client.PropertyDashboardClient;
import com.pms.dashboard.client.ReservationDashboardClient;
import com.pms.dashboard.config.DashboardProperties;
import com.pms.dashboard.dto.response.FrontdeskDashboardResponse;
import com.pms.dashboard.service.model.DashboardModels;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.pms.reservation.repository.ReservationBookingRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class FrontdeskDashboardServiceImplTest {

    @Test
    void shouldBuildDashboardWithMergedData() {
        HousekeepingDashboardClient housekeepingClient = Mockito.mock(HousekeepingDashboardClient.class);
        InventoryDashboardClient inventoryClient = Mockito.mock(InventoryDashboardClient.class);
        PropertyDashboardClient propertyClient = Mockito.mock(PropertyDashboardClient.class);
        ReservationDashboardClient reservationClient = Mockito.mock(ReservationDashboardClient.class);
        ReservationBookingRepository reservationBookingRepository = Mockito.mock(ReservationBookingRepository.class);

        UUID propertyId = UUID.randomUUID();
        LocalDate businessDate = LocalDate.of(2026, 8, 3);
        UUID roomTypeId = UUID.randomUUID();

        Mockito.when(housekeepingClient.fetchDashboard(propertyId, businessDate))
                .thenReturn(Mono.just(new DashboardModels.HousekeepingDashboardData(100, 20, 10, 40, 10, 2, 1, 5, 3, 12, 8)));

        List<DashboardModels.HousekeepingRoomData> todayRooms = List.of(
                new DashboardModels.HousekeepingRoomData("King", roomTypeId, "INSPECTED", "VACANT", true),
                new DashboardModels.HousekeepingRoomData("King", roomTypeId, "DIRTY", "VACANT", false),
                new DashboardModels.HousekeepingRoomData("King", roomTypeId, "CLEAN", "OCCUPIED", true)
        );
        Mockito.when(housekeepingClient.fetchRooms(propertyId, businessDate)).thenReturn(Mono.just(todayRooms));
        Mockito.when(housekeepingClient.fetchRooms(propertyId, businessDate.plusDays(1))).thenReturn(Mono.just(todayRooms));

        Mockito.when(propertyClient.fetchRoomTypes(propertyId))
                .thenReturn(Mono.just(List.of(new DashboardModels.PropertyRoomTypeData(roomTypeId, "KING", "Standard King"))));

        Mockito.when(inventoryClient.fetchDaily(propertyId, roomTypeId, businessDate))
                .thenReturn(Mono.just(new DashboardModels.InventoryDailyData(25, 15, 3, 7)));

        Mockito.when(reservationClient.fetchFlow(propertyId, businessDate))
                .thenReturn(Mono.just(new DashboardModels.ReservationFlowData(14, 9)));

        DashboardProperties properties = new DashboardProperties();
        properties.setTimeoutMs(3000);

        FrontdeskDashboardServiceImpl service = new FrontdeskDashboardServiceImpl(
                housekeepingClient,
                inventoryClient,
                propertyClient,
                reservationClient,
                properties,
                reservationBookingRepository
        );

        FrontdeskDashboardResponse response = service.getDashboard(propertyId, businessDate);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(propertyId, response.propertyId());
        Assertions.assertEquals(7, response.kpis().availableTonight());
        Assertions.assertEquals(50, response.kpis().occupiedTonight());
        Assertions.assertEquals(1, response.roomStatusOverview().size());
        Assertions.assertEquals(new BigDecimal("2500"), response.revenue().roomRevenue());
        Assertions.assertEquals("OK", response.sources().get("housekeepingSummary"));
    }
}

