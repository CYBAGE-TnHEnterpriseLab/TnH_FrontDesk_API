package com.pms.housekeeping.specifications;

import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.entity.HousekeepingPriority;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class HousekeepingRoomSpecification {

    private HousekeepingRoomSpecification() {
    }

    public static Specification<HousekeepingRoomDayStatus> build(
            HousekeepingRoomFilterRequest request) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Mandatory filters

            predicates.add(
                    cb.equal(root.get("propertyId"), request.propertyId())
            );

            predicates.add(
                    cb.equal(root.get("businessDate"), request.businessDate())
            );

            // Search

            if (request.search() != null && !request.search().isBlank()) {

                String keyword = "%" + request.search().trim().toLowerCase() + "%";

                Predicate roomNumber = cb.like(
                        cb.lower(root.get("roomNumber")),
                        keyword
                );

                Predicate guestName = cb.like(
                        cb.lower(root.get("guestDisplayName")),
                        keyword
                );

                predicates.add(
                        cb.or(roomNumber, guestName)
                );
            }

            // Room Type

            if (request.roomTypeId() != null) {

                predicates.add(
                        cb.equal(root.get("roomTypeId"), request.roomTypeId())
                );

            }

            // Floor
            // Remove this if floor belongs to RoomMaster table.

            if (request.floor() != null &&
                    !request.floor().isBlank()) {

                predicates.add(
                        cb.equal(
                                cb.lower(root.get("floor")),
                                request.floor().toLowerCase()
                        )
                );

            }

            // Attendant

            if (request.attendant() != null &&
                    !request.attendant().isBlank()) {

                predicates.add(
                        cb.equal(
                                cb.lower(root.get("attendantName")),
                                request.attendant().toLowerCase()
                        )
                );

            }

            // Cleaning Status

            if (request.cleaningStatus() != null &&
                    !request.cleaningStatus().isEmpty()) {

                predicates.add(
                        root.get("cleaningStatus")
                                .in(request.cleaningStatus())
                );

            }

            // Front Office Status

            if (request.frontOfficeStatus() != null &&
                    !request.frontOfficeStatus().isEmpty()) {

                predicates.add(
                        root.get("frontOfficeStatus")
                                .in(request.frontOfficeStatus())
                );

            }

            // Reservation Status

            if (request.reservationStatus() != null &&
                    !request.reservationStatus().isEmpty()) {

                predicates.add(
                        root.get("reservationStatus")
                                .in(request.reservationStatus())
                );

            }

            // VIP

            if (request.priority() != null) {

                predicates.add(
                        cb.equal(
                                root.get("priority"),
                                request.priority()
                        )
                );

            }
            return cb.and(predicates.toArray(new Predicate[0]));

        };

    }

}