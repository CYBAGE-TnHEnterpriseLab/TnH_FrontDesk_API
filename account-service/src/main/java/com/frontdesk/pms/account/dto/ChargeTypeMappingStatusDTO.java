package com.frontdesk.pms.account.dto;

public class ChargeTypeMappingStatusDTO {
    private String name;
    private String description;
    private String mappedAccount;
    private String status;

    public ChargeTypeMappingStatusDTO(String name, String description, String mappedAccount, String status) {
        this.name = name;
        this.description = description;
        this.mappedAccount = mappedAccount;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
