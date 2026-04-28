package com.frontdesk.pms.content.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "properties", schema = "public")
@Getter
@Setter
public class PropertyReference {

    @Id
    private UUID id;
}
