package com.pharmacy.saas.tenant.dto;

import com.pharmacy.saas.tenant.entity.Tenant;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TenantResponse {

    private UUID id;
    private String tenantCode;
    private String pharmacyName;
    private String email;
    private String mobile;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String status;
    private String subStatus;
    private Instant createdAt;
    private Instant updatedAt;

    public static TenantResponse from(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .tenantCode(tenant.getTenantCode())
                .pharmacyName(tenant.getPharmacyName())
                .email(tenant.getEmail())
                .mobile(tenant.getMobile())
                .address(tenant.getAddress())
                .city(tenant.getCity())
                .state(tenant.getState())
                .country(tenant.getCountry())
                .postalCode(tenant.getPostalCode())
                .status(tenant.getStatus().name())
                .subStatus(tenant.getSubStatus())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}