package org.example.petinside.admin.dto;

import java.time.LocalDate;

public record DailyStatisticsResponse(
        LocalDate date,
        long newUserCount,
        long newPostCount,
        long activeUserCount
) {
}
