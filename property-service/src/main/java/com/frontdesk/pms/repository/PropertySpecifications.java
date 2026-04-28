package com.frontdesk.pms.repository;

import com.frontdesk.common.enums.PropertyStatus;
import com.frontdesk.pms.entity.Property;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;

public final class PropertySpecifications {

    private PropertySpecifications() {
    }

    public static Specification<Property> nameContainsIgnoreCase(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Property> timeZoneEquals(String timeZone) {
        return (root, query, cb) -> cb.equal(root.get("timeZone"), timeZone);
    }

    public static Specification<Property> statusEquals(PropertyStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Property> checkInTimeGte(LocalTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("checkInTime"), from);
    }

    public static Specification<Property> checkInTimeLte(LocalTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("checkInTime"), to);
    }
}

