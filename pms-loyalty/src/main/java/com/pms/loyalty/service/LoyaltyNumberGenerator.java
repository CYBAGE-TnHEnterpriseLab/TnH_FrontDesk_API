package com.pms.loyalty.service;

import com.pms.loyalty.repository.LoyaltyMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyNumberGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    private final LoyaltyMemberRepository loyaltyMemberRepository;
    private final String prefix;

    public LoyaltyNumberGenerator(LoyaltyMemberRepository loyaltyMemberRepository,
                                  @Value("${loyalty.number.prefix:LOY}") String prefix) {
        this.loyaltyMemberRepository = loyaltyMemberRepository;
        this.prefix = prefix;
    }

    public String generateLoyaltyNumber() {
        BigInteger seq = (BigInteger) entityManager.createNativeQuery("SELECT nextval('loyalty_number_seq')")
                .getSingleResult();
        String number = prefix + String.format("%08d", seq);
        int attempts = 0;
        while (loyaltyMemberRepository.findByLoyaltyNumber(number).isPresent() && attempts++ < 10) {
            seq = (BigInteger) entityManager.createNativeQuery("SELECT nextval('loyalty_number_seq')")
                    .getSingleResult();
            number = prefix + String.format("%08d", seq);
        }
        return number;
    }
}
