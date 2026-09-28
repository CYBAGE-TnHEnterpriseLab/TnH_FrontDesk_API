package com.frontdesk.pms.rate_management.entity;

import com.frontdesk.pms.rate_management.enums.DifferentialType;
import com.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class MasterRoomRoomTypeMapping extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_room_id", nullable = false)
    private MasterRoom masterRoom;

    @Column(nullable = false)
    private Long roomTypeId; // ID from external Room Type service

    @Enumerated(EnumType.STRING)
    @Column(name = "differential_type")
    private DifferentialType differentialType;

    @Column(name = "differential_value", precision = 10, scale = 2)
    private BigDecimal differentialValue;
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MasterRoom getMasterRoom() {
        return masterRoom;
    }

    public void setMasterRoom(MasterRoom masterRoom) {
        this.masterRoom = masterRoom;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public DifferentialType getDifferentialType() {
        return differentialType;
    }

    public void setDifferentialType(DifferentialType differentialType) {
        this.differentialType = differentialType;
    }

    public BigDecimal getDifferentialValue() {
        return differentialValue;
    }

    public void setDifferentialValue(BigDecimal differentialValue) {
        this.differentialValue = differentialValue;
    }
}