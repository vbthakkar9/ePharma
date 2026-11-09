package com.pharmacy.saas.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class AssignRolesRequest {

    @NotNull
    @NotEmpty
    private Set<UUID> roleIds;
}