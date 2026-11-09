package com.pharmacy.saas.tenant.entity;

import com.pharmacy.saas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Tenant extends BaseEntity {

    @Column(name = "tenant_code", nullable = false, unique = true, length = 50)
    private String tenantCode;

    @Column(name = "pharmacy_name", nullable = false, length = 255)
    private String pharmacyName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String mobile;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TenantStatus status = TenantStatus.TRIAL;

    @Column(name = "sub_status", length = 50)
    private String subStatus;

    public enum TenantStatus {
        ACTIVE, INACTIVE, SUSPENDED, TRIAL
    }
}