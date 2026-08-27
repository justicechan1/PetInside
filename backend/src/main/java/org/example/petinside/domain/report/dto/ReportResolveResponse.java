package org.example.petinside.domain.report.dto;

import org.example.petinside.domain.report.entity.ReportStatus;

public record ReportResolveResponse(
        Long reportId,
        ReportStatus status,
        Long targetId
) {
}
