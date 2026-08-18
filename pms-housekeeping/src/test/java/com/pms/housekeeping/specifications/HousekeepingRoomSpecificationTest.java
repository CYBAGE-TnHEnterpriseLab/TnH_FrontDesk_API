package com.pms.housekeeping.specifications;

import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingPriority;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatus;
import com.pms.housekeeping.entity.ReservationStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HousekeepingRoomSpecificationTest {

    @Test
    void build_shouldApplyAllOptionalFilters() {
        HousekeepingRoomFilterRequest request = new HousekeepingRoomFilterRequest(
                UUID.randomUUID(),
                LocalDate.of(2026, 8, 18),
                " suite ",
                List.of(CleaningStatus.CLEAN, CleaningStatus.INSPECTED),
                List.of(FrontOfficeStatus.VACANT),
                List.of(ReservationStatus.NOT_RESERVED),
                UUID.randomUUID(),
                "1",
                "Anna",
                HousekeepingPriority.VIP,
                0,
                25,
                "roomNumber",
                "desc"
        );

        Root<HousekeepingRoomDayStatus> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> path = mock(Path.class);
        Expression<String> lowered = mock(Expression.class);
        Predicate propertyPredicate = mock(Predicate.class);
        Predicate datePredicate = mock(Predicate.class);
        Predicate searchPredicate = mock(Predicate.class);
        Predicate roomTypePredicate = mock(Predicate.class);
        Predicate floorPredicate = mock(Predicate.class);
        Predicate attendantPredicate = mock(Predicate.class);
        Predicate cleaningPredicate = mock(Predicate.class);
        Predicate frontOfficePredicate = mock(Predicate.class);
        Predicate reservationPredicate = mock(Predicate.class);
        Predicate priorityPredicate = mock(Predicate.class);
        Predicate orPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.get(anyString())).thenReturn(path);
        when(cb.equal(any(), any())).thenReturn(propertyPredicate, datePredicate, roomTypePredicate, floorPredicate, attendantPredicate, priorityPredicate);
        when(cb.lower(any(Expression.class))).thenReturn(lowered);
        when(cb.like(any(Expression.class), anyString())).thenReturn(searchPredicate);
        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(orPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(finalPredicate);
        when(root.get("cleaningStatus")).thenReturn(path);
        when(root.get("frontOfficeStatus")).thenReturn(path);
        when(root.get("reservationStatus")).thenReturn(path);
        when(path.in(request.cleaningStatus())).thenReturn(cleaningPredicate);
        when(path.in(request.frontOfficeStatus())).thenReturn(frontOfficePredicate);
        when(path.in(request.reservationStatus())).thenReturn(reservationPredicate);

        Specification<HousekeepingRoomDayStatus> specification = HousekeepingRoomSpecification.build(request);
        Predicate result = specification.toPredicate(root, query, cb);

        assertThat(result).isSameAs(finalPredicate);
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void build_shouldOnlyApplyMandatoryFiltersWhenOptionalFieldsAreMissing() {
        HousekeepingRoomFilterRequest request = new HousekeepingRoomFilterRequest(
                UUID.randomUUID(),
                LocalDate.of(2026, 8, 18),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Root<HousekeepingRoomDayStatus> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> path = mock(Path.class);
        Predicate propertyPredicate = mock(Predicate.class);
        Predicate datePredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.get(anyString())).thenReturn(path);
        when(cb.equal(any(), any())).thenReturn(propertyPredicate, datePredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(finalPredicate);

        Predicate result = HousekeepingRoomSpecification.build(request).toPredicate(root, query, cb);

        assertThat(result).isSameAs(finalPredicate);
        verify(cb).and(any(Predicate[].class));
    }
}


