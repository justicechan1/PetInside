package org.example.petinside.domain.report.dto;

import org.example.petinside.domain.report.entity.Report;
import org.example.petinside.domain.report.entity.ReportReason;
import org.example.petinside.domain.report.entity.ReportStatus;
import org.example.petinside.domain.report.entity.ReportTargetType;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        ReportReason reason,
        String detail,
        ReportStatus status,
        Long reporterId,
        String reporterNickname,
        LocalDateTime createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getReporter().getId(),
                report.getReporter().getNickname(),
                report.getCreatedAt()
        );
    }
}
