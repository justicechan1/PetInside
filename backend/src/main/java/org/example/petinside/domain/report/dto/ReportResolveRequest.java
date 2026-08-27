package org.example.petinside.domain.report.dto;

import jakarta.validation.constraints.NotNull;
import org.example.petinside.domain.report.entity.ReportAction;

public record ReportResolveRequest(
        @NotNull(message = "처리 액션은 필수입니다.") ReportAction action
) {
}
