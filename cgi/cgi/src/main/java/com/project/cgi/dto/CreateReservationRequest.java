package com.project.cgi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateReservationRequest(
	@NotNull Long tableId,
	Long mergedTableId,
	@NotNull LocalDateTime start,
	@NotNull @Min(1) @Max(20) Integer partySize,
	@NotBlank @Size(max = 128) String guestName
) {
}
