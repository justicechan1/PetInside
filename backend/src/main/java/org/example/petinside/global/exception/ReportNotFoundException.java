package org.example.petinside.global.exception;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(Long reportId) {
        super("존재하지 않는 신고입니다. id=" + reportId);
    }
}
