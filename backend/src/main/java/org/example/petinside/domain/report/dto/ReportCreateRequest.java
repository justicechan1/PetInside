package org.example.petinside.domain.report.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.petinside.domain.report.entity.ReportReason;
import org.example.petinside.domain.report.entity.ReportTargetType;

public record ReportCreateRequest(
        @NotNull(message = "신고 대상 종류는 필수입니다.") ReportTargetType targetType,
        @NotNull(message = "신고 대상 ID는 필수입니다.") Long targetId,
        @NotNull(message = "신고 사유는 필수입니다.") ReportReason reason,
        @Size(max = 500, message = "상세 사유는 500자를 초과할 수 없습니다.") String detail
) {
}
