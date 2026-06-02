package com.hotel.pms.frontdesk.guestlisting.spec;

import com.hotel.pms.frontdesk.guestlisting.dto.ArrivalSearchRequestDto;
import com.hotel.pms.frontdesk.guestlisting.entity.ArrivalRecord;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ArrivalSpecification {

    private ArrivalSpecification() {
    }

    public static Specification<ArrivalRecord> byCriteria(ArrivalSearchRequestDto criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("propertyId"), criteria.getPropertyId()));
            predicates.add(cb.equal(root.get("businessDate"), criteria.getBusinessDate()));
            predicates.add(cb.equal(root.get("checkInDate"), criteria.getBusinessDate()));

            if (StringUtils.hasText(criteria.getStatus())) {
                predicates.add(cb.equal(cb.lower(root.get("status")), criteria.getStatus().toLowerCase()));
            }
            if (StringUtils.hasText(criteria.getReservationType())) {
                predicates.add(cb.equal(cb.lower(root.get("reservationType")), criteria.getReservationType().toLowerCase()));
            }
            if (StringUtils.hasText(criteria.getCity())) {
                predicates.add(cb.like(cb.lower(root.get("city")), like(criteria.getCity())));
            }
            if (StringUtils.hasText(criteria.getRoomStatus())) {
                predicates.add(cb.equal(cb.lower(root.get("roomStatus")), criteria.getRoomStatus().toLowerCase()));
            }
            if (StringUtils.hasText(criteria.getCorporateCode())) {
                predicates.add(cb.equal(cb.lower(root.get("corporateCode")), criteria.getCorporateCode().toLowerCase()));
            }
            if (StringUtils.hasText(criteria.getRoomType())) {
                predicates.add(cb.like(cb.lower(root.get("roomType")), like(criteria.getRoomType())));
            }
            if (StringUtils.hasText(criteria.getCompany())) {
                predicates.add(cb.like(cb.lower(root.get("company")), like(criteria.getCompany())));
            }
            if (StringUtils.hasText(criteria.getSharingStatus())) {
                predicates.add(cb.equal(cb.lower(root.get("sharingStatus")), criteria.getSharingStatus().toLowerCase()));
            }
            if (StringUtils.hasText(criteria.getLoyaltyMembershipStatus())) {
                predicates.add(cb.like(cb.lower(root.get("loyaltyMembershipStatus")), like(criteria.getLoyaltyMembershipStatus())));
            }

            if (StringUtils.hasText(criteria.getSearch())) {
                String wildcard = like(criteria.getSearch());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), wildcard),
                        cb.like(cb.lower(root.get("lastName")), wildcard),
                        cb.like(cb.lower(root.get("confirmationNumber")), wildcard),
                        cb.like(cb.lower(root.get("roomNo")), wildcard),
                        cb.like(cb.lower(root.get("company")), wildcard),
                        cb.like(cb.lower(root.get("city")), wildcard)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String like(String value) {
        return "%" + value.toLowerCase() + "%";
    }
}
