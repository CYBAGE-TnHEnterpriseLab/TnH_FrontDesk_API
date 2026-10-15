package com.pms.loyalty.service;

import com.pms.loyalty.repository.LoyaltyMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyNumberGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    private final LoyaltyMemberRepository loyaltyMemberRepository;
    private final String prefix;
    private final String loyaltyNumberSequence;

    public LoyaltyNumberGenerator(LoyaltyMemberRepository loyaltyMemberRepository,
                                  @Value("${loyalty.number.prefix:LOY}") String prefix,
                                  @Value("${spring.jpa.properties.hibernate.default_schema:pms_loyalty}") String defaultSchema) {
        this.loyaltyMemberRepository = loyaltyMemberRepository;
        this.prefix = prefix;
        this.loyaltyNumberSequence = defaultSchema.trim() + ".loyalty_number_seq";
    }

    public String generateLoyaltyNumber() {
        long seq = nextSequenceValue();
        String number = prefix + String.format("%08d", seq);
        int attempts = 0;
        while (loyaltyMemberRepository.findByLoyaltyNumber(number).isPresent()) {
            if (attempts++ >= 10) {
                throw new IllegalStateException("Unable to generate a unique loyalty number");
            }
            seq = nextSequenceValue();
            number = prefix + String.format("%08d", seq);
        }
        return number;
    }

    /**
     * Reads the sequence through a schema-qualified name. Hibernate qualifies entity and JPQL queries with
     * hibernate.default_schema, but a raw native SQL string is sent to the connection unchanged and is
     * resolved against the connection search_path only, so an unqualified name fails whenever the
     * datasource URL does not set CurrentSchema.
     *
     * nextval returns bigint, which the PostgreSQL driver maps to Long. The value is read as a Number
     * rather than cast to a fixed numeric type, because the driver and Hibernate have historically
     * returned different numeric types for the same query.
     */
    private long nextSequenceValue() {
        Object value = entityManager
                .createNativeQuery("SELECT nextval('" + loyaltyNumberSequence + "')")
                .getSingleResult();
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Unexpected value returned by " + loyaltyNumberSequence + ": " + value);
        }
        return number.longValue();
    }
}
