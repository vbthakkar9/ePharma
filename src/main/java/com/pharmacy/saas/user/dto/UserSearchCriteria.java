package com.pharmacy.saas.user.dto;

import com.pharmacy.saas.user.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserSearchCriteria {

    private String search;
    private UserStatus status;
    private UUID roleId;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
    @Builder.Default
    private String sort = "createdAt,DESC";
}