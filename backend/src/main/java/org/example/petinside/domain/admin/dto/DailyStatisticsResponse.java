package org.example.petinside.domain.admin.dto;

import java.time.LocalDate;

public record DailyStatisticsResponse(
        LocalDate date,
        long newUserCount,
        long newPostCount,
        long activeUserCount
) {
}
