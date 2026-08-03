package Policy_Management.Policy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import Policy_Management.Policy.dto.Status;

@Data
@NoArgsConstructor
@Entity
@Table(name = "policies")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_name", nullable = false, length = 200)
    private String policyName;

    @Column(name = "policy_type", nullable = false)
    private String policyType;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "used_by", nullable = false)
    private String usedBy;

    @Column(name = "policy_code", nullable = false)
    private String policyCode;

    @Column(name = "policy_category", nullable = false)
    private String policyCategory;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "action")
    private String action;

    @Column(name = "policy_count")
    private int policyCount;
}
