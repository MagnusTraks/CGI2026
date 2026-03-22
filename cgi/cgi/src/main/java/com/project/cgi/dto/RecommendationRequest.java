package com.project.cgi.dto;

import com.project.cgi.domain.Zone;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RecommendationRequest(
	@NotNull LocalDateTime start,
	@NotNull @Min(1) @Max(20) Integer partySize,
	Zone zone,
	boolean wantsPrivacy,
	boolean wantsWindow,
	boolean wantsNearKids,
	boolean wantsAccessible
) {
}
