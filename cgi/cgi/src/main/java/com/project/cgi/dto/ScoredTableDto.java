package com.project.cgi.dto;

public record ScoredTableDto(
	long tableId,
	String code,
	double score,
	String explanation,
	Long mergedTableId,
	String mergedTableCode,
	boolean merged
) {
}
