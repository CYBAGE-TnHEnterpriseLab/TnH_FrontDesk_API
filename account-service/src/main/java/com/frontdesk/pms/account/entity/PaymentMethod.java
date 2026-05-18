package com.frontdesk.pms.account.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
public class PaymentMethod {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private UUID propertyId;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private boolean allowRefund;

    @Column(nullable = false)
    private boolean active = true;
}
