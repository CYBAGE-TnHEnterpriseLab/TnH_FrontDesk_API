package com.frontdesk.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID propertyId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}