package com.pms.housekeeping.specifications;

import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class HousekeepingRoomSpecification {

    private HousekeepingRoomSpecification() {
    }

    public static Specification<HousekeepingRoomDayStatus> build(
            HousekeepingRoomFilterRequest request
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            // =====================================================
            // PROPERTY
            // =====================================================

            predicates.add(
                    cb.equal(
                            root.get("propertyId"),
                            request.propertyId()
                    )
            );

            // =====================================================
            // BUSINESS DATE
            // =====================================================

            predicates.add(
                    cb.equal(
                            root.get("businessDate"),
                            request.businessDate()
                    )
            );

            // =====================================================
            // SEARCH
            // =====================================================

            if (request.search() != null
                    && !request.search().isBlank()) {

                String keyword =
                        "%"
                                + request.search()
                                .trim()
                                .toLowerCase()
                                + "%";

                Predicate roomNumber =
                        cb.like(
                                cb.lower(
                                        root.get("roomNumber")
                                ),
                                keyword
                        );

                Predicate guestName =
                        cb.like(
                                cb.lower(
                                        root.get("guestDisplayName")
                                ),
                                keyword
                        );

                Predicate confirmationId =
                        cb.like(
                                cb.lower(
                                        root.get("confirmationId")
                                ),
                                keyword
                        );

                predicates.add(
                        cb.or(
                                roomNumber,
                                guestName,
                                confirmationId
                        )
                );
            }

            // =====================================================
            // ROOM TYPE
            // =====================================================

            if (request.roomTypeId() != null) {

                predicates.add(
                        cb.equal(
                                root.get("roomTypeId"),
                                request.roomTypeId()
                        )
                );
            }

            // =====================================================
            // FLOOR
            // =====================================================

            if (request.floor() != null
                    && !request.floor().isBlank()) {

                predicates.add(
                        cb.equal(
                                cb.lower(
                                        root.get("floor")
                                ),
                                request.floor()
                                        .trim()
                                        .toLowerCase()
                        )
                );
            }

            // =====================================================
            // ATTENDANT
            // =====================================================

            if (request.attendant() != null
                    && !request.attendant().isBlank()) {

                predicates.add(
                        cb.equal(
                                cb.lower(
                                        root.get("attendantName")
                                ),
                                request.attendant()
                                        .trim()
                                        .toLowerCase()
                        )
                );
            }

            // =====================================================
            // CLEANING STATUS
            // =====================================================

            if (request.cleaningStatus() != null
                    && !request.cleaningStatus().isEmpty()) {

                predicates.add(
                        root.get("cleaningStatus")
                                .in(request.cleaningStatus())
                );
            }

            // =====================================================
            // FRONT OFFICE STATUS
            // =====================================================

            if (request.frontOfficeStatus() != null
                    && !request.frontOfficeStatus().isEmpty()) {

                predicates.add(
                        root.get("frontOfficeStatus")
                                .in(request.frontOfficeStatus())
                );
            }

            // =====================================================
            // RESERVATION STATUS
            // =====================================================

            if (request.reservationStatus() != null
                    && !request.reservationStatus().isEmpty()) {

                predicates.add(
                        root.get("reservationStatus")
                                .in(request.reservationStatus())
                );
            }

            // =====================================================
            // PRIORITY
            // =====================================================

            if (request.priority() != null) {

                predicates.add(
                        cb.equal(
                                root.get("priority"),
                                request.priority()
                        )
                );
            }

            return cb.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }
}