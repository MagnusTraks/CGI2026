package com.project.cgi.dto;

import java.util.List;

public record RecommendationResponse(
	Long bestTableId,
	Long bestMergedTableId,
	List<ScoredTableDto> rankedTables
) {
}
