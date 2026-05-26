package com.frontdesk.pms.account.entity;

import com.frontdesk.common.entity.BaseEntity;
import com.frontdesk.common.enums.AccountType;
import com.frontdesk.common.enums.LedgerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "chart_of_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chart_of_accounts_property_id_code",
                        columnNames = {"property_id", "code"}
                ),
                @UniqueConstraint(
                        name = "uk_chart_of_accounts_property_id_name",
                        columnNames = {"property_id", "name"}
                )
        }
)
@Getter
@Setter
public class ChartOfAccount extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
        private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LedgerType ledgerType;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
