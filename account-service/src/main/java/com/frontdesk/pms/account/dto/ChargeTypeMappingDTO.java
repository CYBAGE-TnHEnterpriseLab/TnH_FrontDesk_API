package com.frontdesk.pms.account.dto;

import java.util.UUID;

public class ChargeTypeMappingDTO {
    private String name;
    private String description;
    private String mappedAccount;

    public ChargeTypeMappingDTO(String name, String description, String mappedAccount) {
        this.name = name;
        this.description = description;
        this.mappedAccount = mappedAccount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMappedAccount() {
        return mappedAccount;
    }

    public void setMappedAccount(String mappedAccount) {
        this.mappedAccount = mappedAccount;
    }
}
