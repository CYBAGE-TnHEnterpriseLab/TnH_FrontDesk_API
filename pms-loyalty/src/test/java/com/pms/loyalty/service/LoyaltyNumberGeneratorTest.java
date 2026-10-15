package com.pms.loyalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.loyalty.repository.LoyaltyMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LoyaltyNumberGeneratorTest {

    private static final String PREFIX = "LOY";

    /**
     * The PostgreSQL driver maps nextval's bigint result to Long, so the generator must not assume a
     * single numeric type. A regression here breaks every enrollment.
     */
    @Test
    void acceptsLongSequenceValues() {
        assertThat(generatorReturning(10_000_000L)).isEqualTo("LOY10000000");
    }

    @Test
    void acceptsOtherNumericSequenceValueTypes() {
        assertThat(generatorReturning(java.math.BigInteger.valueOf(10_000_001L))).isEqualTo("LOY10000001");
        assertThat(generatorReturning(Integer.valueOf(10_000_002))).isEqualTo("LOY10000002");
    }

    @Test
    void qualifiesTheSequenceWithTheConfiguredSchema() {
        EntityManager entityManager = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<Object> query = mock(TypedQuery.class);
        when(entityManager.createNativeQuery("SELECT nextval('custom_schema.loyalty_number_seq')"))
                .thenReturn(query);
        when(query.getSingleResult()).thenReturn(10_000_003L);

        LoyaltyMemberRepository memberRepository = mock(LoyaltyMemberRepository.class);
        LoyaltyNumberGenerator generator = new LoyaltyNumberGenerator(memberRepository, PREFIX, "custom_schema");
        ReflectionTestUtils.setField(generator, "entityManager", entityManager);

        assertThat(generator.generateLoyaltyNumber()).isEqualTo("LOY10000003");
    }

    @Test
    void retriesWhenGeneratedNumberAlreadyExists() {
        EntityManager entityManager = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<Object> query = mock(TypedQuery.class);
        when(entityManager.createNativeQuery("SELECT nextval('pms_loyalty.loyalty_number_seq')"))
                .thenReturn(query);
        when(query.getSingleResult()).thenReturn(10_000_004L, 10_000_005L);

        LoyaltyMemberRepository memberRepository = mock(LoyaltyMemberRepository.class);
        when(memberRepository.findByLoyaltyNumber("LOY10000004"))
                .thenReturn(Optional.of(com.pms.loyalty.entity.LoyaltyMember.builder().build()));
        when(memberRepository.findByLoyaltyNumber("LOY10000005")).thenReturn(Optional.empty());

        LoyaltyNumberGenerator generator = new LoyaltyNumberGenerator(memberRepository, PREFIX, "pms_loyalty");
        ReflectionTestUtils.setField(generator, "entityManager", entityManager);

        assertThat(generator.generateLoyaltyNumber()).isEqualTo("LOY10000005");
    }

    private String generatorReturning(Object sequenceValue) {
        EntityManager entityManager = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<Object> query = mock(TypedQuery.class);
        when(entityManager.createNativeQuery("SELECT nextval('pms_loyalty.loyalty_number_seq')"))
                .thenReturn(query);
        when(query.getSingleResult()).thenReturn(sequenceValue);

        LoyaltyNumberGenerator generator = new LoyaltyNumberGenerator(
                mock(LoyaltyMemberRepository.class), PREFIX, "pms_loyalty");
        ReflectionTestUtils.setField(generator, "entityManager", entityManager);
        return generator.generateLoyaltyNumber();
    }
}
