package com.project.cgi.dto;

import java.time.LocalDateTime;

public record ReservationViewDto(
	long id,
	long tableId,
	String tableCode,
	Long mergedTableId,
	String mergedTableCode,
	LocalDateTime startTime,
	LocalDateTime endTime,
	int guestCount,
	String guestName
) {
}
