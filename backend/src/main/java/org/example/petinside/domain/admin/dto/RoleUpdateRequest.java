package org.example.petinside.domain.admin.dto;

import jakarta.validation.constraints.NotNull;
import org.example.petinside.domain.admin.entity.Role;

public record RoleUpdateRequest(@NotNull Role role) {
}
