package com.pms.reservation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * Releasing nights that are no longer part of a stay is driven off an inclusive date range. Clearing that
 * range must cover its departure date, otherwise the last night keeps the guest's OCCUPIED/DEPARTURE row and
 * the room looks booked for a night the guest no longer holds.
 */
@ExtendWith(MockitoExtension.class)
class HousekeepingRoomStatusClientTest {

    private static final UUID PROPERTY_ID = UUID.fromString("b9ca4057-4b8b-462a-b1e9-c9e47aafe488");
    private static final String ROOM_NUMBER = "101";

    @Mock
    private RestTemplate restTemplate;

    private HousekeepingRoomStatusClient client;

    @BeforeEach
    void setUp() throws Exception {
        client = new HousekeepingRoomStatusClient(restTemplate);
        var baseUrl = HousekeepingRoomStatusClient.class.getDeclaredField("baseUrl");
        baseUrl.setAccessible(true);
        baseUrl.set(client, "http://housekeeping");
    }

    @Test
    void clearReservationStayClearsEveryNightFromTheDayAfterTheNewDepartureToTheOldDeparture() {
        // A stay booked Oct 10 to Oct 15 shortened to Oct 10 to Oct 11 releases Oct 12 through Oct 15.
        client.clearReservationStay(PROPERTY_ID, LocalDate.parse("2026-10-12"), LocalDate.parse("2026-10-15"),
                ROOM_NUMBER);

        assertThat(businessDates())
                .containsExactly(LocalDate.parse("2026-10-12"), LocalDate.parse("2026-10-13"),
                        LocalDate.parse("2026-10-14"), LocalDate.parse("2026-10-15"));
        assertThat(record("frontOfficeStatus")).containsOnly("VACANT");
        assertThat(record("reservationStatus")).containsOnly("NOT_RESERVED");
        assertThat(record("cleaningStatus")).containsOnly("DIRTY");
        assertThat(record("guestDisplayName")).containsOnlyNulls();
        assertThat(record("confirmationId")).containsOnlyNulls();
    }

    @Test
    void clearReservationStayCoversTheDepartureDateOfAsingleDayRange() {
        client.clearReservationStay(PROPERTY_ID, LocalDate.parse("2026-10-15"), LocalDate.parse("2026-10-15"),
                ROOM_NUMBER);

        assertThat(businessDates()).containsExactly(LocalDate.parse("2026-10-15"));
    }

    @Test
    void clearReservationStaySkipsAnEmptyRange() {
        // The stay already ended by then, so there is nothing to clear.
        client.clearReservationStay(PROPERTY_ID, LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-15"),
                ROOM_NUMBER);

        verifyNoInteractions(restTemplate);
    }

    private List<LocalDate> businessDates() {
        return record("businessDate");
    }

    private <T> List<T> record(String accessor) {
        ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, atLeastOnce()).exchange(
                any(String.class), any(HttpMethod.class), captor.capture(), eq(String.class));
        return captor.getAllValues().stream()
                .map(HttpEntity::getBody)
                .map(body -> invoke(body, accessor))
                .map(accessorValue -> {
                    @SuppressWarnings("unchecked")
                    T value = (T) accessorValue;
                    return value;
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Object target, String accessor) {
        try {
            var method = target.getClass().getDeclaredMethod(accessor);
            method.setAccessible(true);
            return (T) method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            throw new AssertionError("Cannot read " + accessor + " from " + target.getClass(), ex);
        }
    }
}
