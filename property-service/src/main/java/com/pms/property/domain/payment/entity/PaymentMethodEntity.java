package com.pms.property.domain.payment.entity;

import com.pms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payment_method")
public class PaymentMethodEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "account_mapping", nullable = false)
    private String accountMapping;

    @Column(name = "allow_refund", nullable = false)
    private Boolean allowRefund;

    @Column(nullable = false)
    private Boolean active;

}



