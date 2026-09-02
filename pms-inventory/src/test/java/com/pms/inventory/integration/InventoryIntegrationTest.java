package com.pms.inventory.integration;

import com.pms.inventory.block.dto.request.CreateInventoryBlockRequest;
import com.pms.inventory.block.entity.InventoryBlock;
import com.pms.inventory.block.enums.InventoryBlockStatus;
import com.pms.inventory.block.repository.InventoryBlockRepository;
import com.pms.inventory.block.service.InventoryBlockService;
import com.pms.inventory.common.exception.InsufficientInventoryException;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import com.pms.inventory.inventory.repository.RoomTypeInventoryDailyRepository;
import com.pms.inventory.reconciliation.dto.InventoryReconciliationRequest;
import com.pms.inventory.reconciliation.service.InventoryReconciliationService;
import com.pms.inventory.reservation.dto.request.ReserveInventoryRequest;
import com.pms.inventory.reservation.dto.response.InventoryReservationResponse;
import com.pms.inventory.reservation.enums.InventoryReservationStatus;
import com.pms.inventory.reservation.repository.InventoryReservationRepository;
import com.pms.inventory.reservation.service.InventoryReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InventoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pms_inventory_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private InventoryReconciliationService reconciliationService;
    @Autowired
    private InventoryReservationService reservationService;
    @Autowired
    private InventoryBlockService blockService;
    @Autowired
    private RoomTypeInventoryDailyRepository dailyRepository;
    @Autowired
    private InventoryReservationRepository reservationRepository;
    @Autowired
    private InventoryBlockRepository blockRepository;

    private String propertyId;
    private String deluxeRoomType;
    private String suiteRoomType;

    @BeforeEach
    void cleanAndSeed() {
        reservationRepository.deleteAll();
        blockRepository.deleteAll();
        dailyRepository.deleteAll();

        propertyId = "property-1";
        deluxeRoomType = "room-type-deluxe";
        suiteRoomType = "room-type-suite";

        reconciliationService.reconcile(new InventoryReconciliationRequest(
                propertyId,
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 23),
                List.of(
                        new InventoryReconciliationRequest.RoomTypeInventoryInput(deluxeRoomType, 1),
                        new InventoryReconciliationRequest.RoomTypeInventoryInput(suiteRoomType, 1)
                )
        ));
    }

    @Test
    void reserveAndReleaseFlowWithIdempotency() {
        String reservationId = "confirmation-1";
        ReserveInventoryRequest request = new ReserveInventoryRequest(
                reservationId,
                propertyId,
                deluxeRoomType,
                deluxeRoomType,
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 22),
                1
        );

        InventoryReservationResponse created = reservationService.reserve(request);
        InventoryReservationResponse duplicate = reservationService.reserve(request);

        assertEquals(false, created.idempotent());
        assertEquals(true, duplicate.idempotent());
        assertEquals(InventoryReservationStatus.RESERVED, duplicate.status());

        InventoryReservationResponse released = reservationService.release(reservationId);
        InventoryReservationResponse releaseDuplicate = reservationService.release(reservationId);

        assertEquals(InventoryReservationStatus.RELEASED, released.status());
        assertEquals(true, releaseDuplicate.idempotent());
    }

    @Test
    void reserveFailsWhenInsufficient() {
        ReserveInventoryRequest first = new ReserveInventoryRequest(
                "confirmation-2", propertyId, deluxeRoomType, deluxeRoomType,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 22), 1
        );
        ReserveInventoryRequest second = new ReserveInventoryRequest(
                "confirmation-3", propertyId, deluxeRoomType, deluxeRoomType,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 22), 1
        );

        reservationService.reserve(first);
        assertThrows(InsufficientInventoryException.class, () -> reservationService.reserve(second));
    }

    @Test
    void roomTypeReassignmentFailsWhenNewInventoryUnavailable() {
        // consume suite first so reassignment to suite cannot be satisfied.
        reservationService.reserve(new ReserveInventoryRequest(
                "confirmation-4", propertyId, suiteRoomType, suiteRoomType,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 22), 1
        ));

        String deluxeReservationId = "confirmation-5";
        reservationService.reserve(new ReserveInventoryRequest(
                deluxeReservationId, propertyId, deluxeRoomType, deluxeRoomType,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 22), 1
        ));

        assertThrows(InsufficientInventoryException.class,
                () -> reservationService.changeAssignedRoomType(
                        deluxeReservationId,
                        new com.pms.inventory.reservation.dto.request.ChangeAssignedRoomTypeRequest(suiteRoomType)
                ));
    }

    @Test
    void inventoryBlockCreationAndRelease() {
        CreateInventoryBlockRequest request = new CreateInventoryBlockRequest(
                propertyId,
                deluxeRoomType,
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 21),
                1,
                "Maintenance"
        );

        var created = blockService.create(request);
        assertNotNull(created.blockId());

        RoomTypeInventoryDaily row = dailyRepository
                .findByPropertyIdAndRoomTypeIdAndBusinessDate(propertyId, deluxeRoomType, LocalDate.of(2026, 7, 20))
                .orElseThrow();
        assertEquals(1, row.getBlockedCount());

        var released = blockService.release(created.blockId());
        assertEquals(InventoryBlockStatus.RELEASED, released.status());

        RoomTypeInventoryDaily afterRelease = dailyRepository
                .findByPropertyIdAndRoomTypeIdAndBusinessDate(propertyId, deluxeRoomType, LocalDate.of(2026, 7, 20))
                .orElseThrow();
        assertEquals(0, afterRelease.getBlockedCount());
    }

    @Test
    void reconciliationIsIdempotentAtBusinessKeyLevel() {
        int first = reconciliationService.reconcile(new InventoryReconciliationRequest(
                propertyId,
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 23),
                List.of(new InventoryReconciliationRequest.RoomTypeInventoryInput(deluxeRoomType, 1))
        ));
        int second = reconciliationService.reconcile(new InventoryReconciliationRequest(
                propertyId,
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 23),
                List.of(new InventoryReconciliationRequest.RoomTypeInventoryInput(deluxeRoomType, 1))
        ));

        assertEquals(3, first);
        assertEquals(3, second);
        assertEquals(6, dailyRepository.findAll().size());
    }

    @Test
    void concurrentReservationScenarioOnlyOneSucceeds() throws Exception {
        String roomType = "room-type-concurrent";
        reconciliationService.reconcile(new InventoryReconciliationRequest(
                propertyId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                List.of(new InventoryReconciliationRequest.RoomTypeInventoryInput(roomType, 1))
        ));

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> task1 = reserveCallable(startLatch, roomType, "confirmation-concurrent-1");
        Callable<Boolean> task2 = reserveCallable(startLatch, roomType, "confirmation-concurrent-2");

        Future<Boolean> f1 = executor.submit(task1);
        Future<Boolean> f2 = executor.submit(task2);

        startLatch.countDown();

        int success = 0;
        success += outcome(f1) ? 1 : 0;
        success += outcome(f2) ? 1 : 0;

        executor.shutdownNow();

        assertEquals(1, success);
        assertEquals(1, reservationRepository.findAll().size());
    }

    private Callable<Boolean> reserveCallable(CountDownLatch startLatch, String roomType, String reservationId) {
        return () -> {
            startLatch.await();
            try {
                reservationService.reserve(new ReserveInventoryRequest(
                        reservationId,
                        propertyId,
                        roomType,
                        roomType,
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 2),
                        1
                ));
                return true;
            } catch (InsufficientInventoryException ex) {
                return false;
            }
        };
    }

    private boolean outcome(Future<Boolean> future) throws Exception {
        try {
            return future.get();
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof InsufficientInventoryException) {
                return false;
            }
            throw ex;
        }
    }
}


