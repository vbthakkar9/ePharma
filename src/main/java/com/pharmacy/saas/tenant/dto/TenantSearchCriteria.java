package com.pharmacy.saas.tenant.dto;

import com.pharmacy.saas.tenant.entity.Tenant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantSearchCriteria {

    private String search;
    private Tenant.TenantStatus status;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
    @Builder.Default
    private String sort = "createdAt,DESC";
}